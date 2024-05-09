package org.move.ide.annotator

import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.codeInsight.daemon.impl.HighlightInfoType
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.wm.WindowManager
import com.intellij.psi.PsiFile
import com.intellij.psi.impl.AnyPsiChangeListener
import com.intellij.psi.impl.PsiManagerImpl
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.util.PathUtil
import com.intellij.util.io.URLUtil
import com.intellij.util.messages.MessageBus
import org.apache.commons.lang3.StringEscapeUtils
import org.jetbrains.annotations.Nls
import org.move.cli.externalLinter.RsExternalLinterWidget
import org.move.cli.externalLinter.parseCompilerErrors
import org.move.cli.runConfigurations.AptosCompileArgs
import org.move.cli.runConfigurations.BlockchainCli.Aptos
import org.move.ide.annotator.RsExternalLinterFilteredMessage.Companion.filterMessage
import org.move.ide.annotator.RsExternalLinterUtils.TEST_MESSAGE
import org.move.lang.MoveFile
import org.move.openapiext.ProjectCache
import org.move.openapiext.checkReadAccessAllowed
import org.move.openapiext.checkReadAccessNotAllowed
import org.move.openapiext.common.isUnitTestMode
import org.move.openapiext.saveAllDocumentsAsTheyAre
import org.move.stdext.capitalized
import org.move.stdext.unwrapOrElse
import java.nio.file.Path
import java.time.Duration
import java.time.Instant
import java.util.concurrent.CompletableFuture

object RsExternalLinterUtils {
    private val LOG: Logger = logger<RsExternalLinterUtils>()
    const val TEST_MESSAGE: String = "RsExternalLint"

    /**
     * Returns (and caches if absent) lazily computed messages from external linter.
     *
     * Note: before applying this result you need to check that the PSI modification stamp of current project has not
     * changed after calling this method.
     *
     * @see PsiModificationTracker.MODIFICATION_COUNT
     */
    fun checkLazily(
        aptosCli: Aptos,
//        toolchain: RsToolchainBase,
        project: Project,
        owner: Disposable,
        workingDirectory: Path,
        args: AptosCompileArgs
    ): Lazy<RsExternalLinterResult?> {
        checkReadAccessAllowed()
        return externalLinterLazyResultCache.getOrPut(project, Key(aptosCli, workingDirectory, args)) {
            // We want to run external linter in background thread and *without* read action.
            // And also we want to cache result of external linter because it is cargo package-global,
            // but annotator can be invoked separately for each file.
            // With `CachedValuesManager` our cached value should be invalidated on any PSI change.
            // Important note about this cache is that modification count will be stored AFTER computation
            // of a value. If we aren't in read action, PSI can be modified during computation of the value
            // and so an outdated value will be cached. So we can't use the cache without read action.
            // What we really want:
            // 1. Store current PSI modification count;
            // 2. Run external linter and retrieve results (in background thread and without read action);
            // 3. Try to cache result use modification count stored in (1). Result can be already outdated here.
            // We get such behavior by storing `Lazy` computation to the cache. Cache result is created in read
            // action, so it will be stored within correct PSI modification count. Then, we will retrieve the value
            // from `Lazy` in a background thread. The value will be computed or retrieved from the already computed
            // `Lazy` value.
            lazy {
                // This code will be executed out of read action in background thread
                if (!isUnitTestMode) checkReadAccessNotAllowed()
                checkWrapped(aptosCli, project, owner, workingDirectory, args)
            }
        }
    }

    private fun checkWrapped(
        aptosCli: Aptos,
        project: Project,
        owner: Disposable,
        workingDirectory: Path,
        args: AptosCompileArgs
    ): RsExternalLinterResult? {
        val widget = WriteAction.computeAndWait<RsExternalLinterWidget?, Throwable> {
            saveAllDocumentsAsTheyAre()
            val statusBar = WindowManager.getInstance().getStatusBar(project)
            statusBar?.getWidget(RsExternalLinterWidget.ID) as? RsExternalLinterWidget
        }

        val future = CompletableFuture<RsExternalLinterResult?>()
        val task =
            object: Task.Backgroundable(project, "Analyzing project with ${args.linter.title}...", true) {

                override fun run(indicator: ProgressIndicator) {
                    widget?.inProgress = true
                    future.complete(check(aptosCli, project, owner, workingDirectory, args))
                }

                override fun onFinished() {
                    widget?.inProgress = false
                }
            }
        ProgressManager.getInstance().runProcessWithProgressAsynchronously(task, EmptyProgressIndicator())
        return future.get()
    }

    private fun check(
        aptosCli: Aptos,
        project: Project,
        owner: Disposable,
        workingDirectory: Path,
        args: AptosCompileArgs
    ): RsExternalLinterResult? {
        ProgressManager.checkCanceled()
        val started = Instant.now()
        val output = aptosCli
            .compileProject(project, owner, args)
            .unwrapOrElse { e ->
                LOG.error(e)
                return null
            }
        val finish = Instant.now()
        ProgressManager.checkCanceled()
        if (output.isCancelled) return null
        return RsExternalLinterResult(output.stdoutLines, Duration.between(started, finish).toMillis())
    }

    private data class Key(
        val aptosCli: Aptos,
        val workingDirectory: Path,
        val args: AptosCompileArgs
    )

    private val externalLinterLazyResultCache =
        ProjectCache<Key, Lazy<RsExternalLinterResult?>>("externalLinterLazyResultCache") {
            PsiModificationTracker.MODIFICATION_COUNT
        }
}

fun MessageBus.createDisposableOnAnyPsiChange(): Disposable {
    val disposable = Disposer.newDisposable("Dispose on PSI change")
    connect(disposable).subscribe(
        PsiManagerImpl.ANY_PSI_CHANGE_TOPIC,
        object: AnyPsiChangeListener {
            override fun beforePsiChanged(isPhysical: Boolean) {
                if (isPhysical) {
                    Disposer.dispose(disposable)
                }
            }
        }
    )
    return disposable
}

fun MutableList<HighlightInfo>.addHighlightsForFile(
    file: MoveFile,
    annotationResult: RsExternalLinterResult,
//    minApplicability: Applicability
) {
//    val cargoPackageOrigin = file.containing?.origin
//    if (cargoPackageOrigin != PackageOrigin.WORKSPACE) return

    val doc = file.viewProvider.document
        ?: error("Can't find document for $file in external linter")

    val filteredMessages = annotationResult.messages
        .mapNotNull { message -> filterMessage(file, doc, message) }
        // Cargo can duplicate some error messages when `--all-targets` attribute is used
        .distinct()
    for (message in filteredMessages) {
        // We can't control what messages cargo generates, so we can't test them well.
        // Let's use the special message for tests to distinguish annotation from external linter
        val highlightBuilder = HighlightInfo.newHighlightInfo(convertSeverity(message.severity))
            .severity(message.severity)
            .description(if (isUnitTestMode) TEST_MESSAGE else message.message)
            .escapedToolTip(message.htmlTooltip)
            .range(message.textRange)
            .needsUpdateOnTyping(true)

//        message.quickFixes
//            .singleOrNull { it.applicability <= minApplicability }
//            ?.let { fix ->
//                val element = fix.startElement ?: fix.endElement
//                val lint = message.lint
//                val actions =  if (element != null && lint != null) createSuppressFixes(element, lint) else emptyArray()
//                val options = convertBatchToSuppressIntentionActions(actions).toList()
//                val displayName = "Aptos external linter"
//                val key = HighlightDisplayKey.findOrRegister(APTOS_EXTERNAL_LINTER_ID, displayName)
//                highlightBuilder.registerFix(fix, options, displayName, fix.textRange, key)
//            }

        highlightBuilder.create()?.let(::add)
    }
}

private fun convertSeverity(severity: HighlightSeverity): HighlightInfoType = when (severity) {
    HighlightSeverity.ERROR -> HighlightInfoType.ERROR
    HighlightSeverity.WARNING -> HighlightInfoType.WARNING
    HighlightSeverity.WEAK_WARNING -> HighlightInfoType.WEAK_WARNING
    HighlightSeverity.GENERIC_SERVER_ERROR_OR_WARNING -> HighlightInfoType.GENERIC_WARNINGS_OR_ERRORS_FROM_SERVER
    else -> HighlightInfoType.INFORMATION
}

private const val APTOS_EXTERNAL_LINTER_ID: String = "AptosExternalLinterOptions"

class RsExternalLinterResult(commandOutput: List<String>, val executionTime: Long) {

    val messages: List<AptosCompilerMessage> = parseCompilerErrors(commandOutput)
}

private data class RsExternalLinterFilteredMessage(
    val severity: HighlightSeverity,
    val textRange: TextRange,
    @Nls val message: String,
    @Nls val htmlTooltip: String,
//    val lint: RsLint.ExternalLinterLint?,
//    val quickFixes: List<ApplySuggestionFix>
) {
    companion object {
        fun filterMessage(
            file: PsiFile,
            document: Document,
            message: AptosCompilerMessage
        ): RsExternalLinterFilteredMessage? {
//            if (message.message.startsWith("aborting due to") || message.message.startsWith("cannot continue")) {
//                return null
//            }

            val severity = when (message.severityLevel) {
                "error" -> HighlightSeverity.ERROR
                "warning" -> HighlightSeverity.WEAK_WARNING
                else -> HighlightSeverity.INFORMATION
            }

            // Some error messages are global, and we *could* show then atop of the editor,
            // but they look rather ugly, so just skip them.
            val span = message.mainSpan ?: return null

            val syntaxErrors = listOf("expected pattern", "unexpected token")
            if (syntaxErrors.any { it in span.label.orEmpty() || it in message.message }) {
                return null
            }

            val spanFilePath = PathUtil.toSystemIndependentName(span.filename)
            if (!file.virtualFile.path.endsWith(spanFilePath)) return null

            val textRange = span.toTextRange(document) ?: return null

            val tooltip = buildString {
                append(formatMessage(StringEscapeUtils.escapeHtml4(message.message)).escapeUrls())
//                val code = message.code.formatAsLink()
//                if (code != null) {
//                    append(" [$code]")
//                }

                with(mutableListOf<String>()) {
                    if (span.label != null && !message.message.startsWith(span.label)) {
                        add(StringEscapeUtils.escapeHtml4(span.label))
                    }

//                    message.children
//                        .filter { it.message.isNotBlank() }
//                        .map { "${it.level.capitalized()}: ${StringEscapeUtils.escapeHtml4(it.message)}" }
//                        .forEach { add(it) }

                    append(joinToString(prefix = "<br>", separator = "<br>") { formatMessage(it) }.escapeUrls())
                }
            }

            return RsExternalLinterFilteredMessage(
                severity,
                textRange,
                message.message.capitalized(),
                tooltip,
//                message.code?.code?.let { RsLint.ExternalLinterLint(it) },
//                message.collectQuickFixes(file, document)
            )
        }
    }
}


private fun String.escapeUrls(): String =
    replace(URL_REGEX) { url -> "<a href='${url.value}'>${url.value}</a>" }

//private val ERROR_REGEX: Regex = """E\d{4}""".toRegex()
private val URL_REGEX: Regex = URLUtil.URL_PATTERN.toRegex()

//private fun ErrorCode?.formatAsLink(): String? {
//    if (this?.code?.matches(ERROR_REGEX) != true) return null
//    return "<a href=\"${RsConstants.ERROR_INDEX_URL}#$code\">$code</a>"
//}

//private fun RustcMessage.collectQuickFixes(file: PsiFile, document: Document): List<ApplySuggestionFix> {
//    val quickFixes = mutableListOf<ApplySuggestionFix>()
//
//    fun go(message: RustcMessage) {
//        val span = message.spans.singleOrNull { it.is_primary && it.isValid() }
//        createQuickFix(file, document, span, message.message)?.let { quickFixes.add(it) }
//        message.children.forEach(::go)
//    }
//
//    go(this)
//    return quickFixes
//}

//private fun createQuickFix(file: PsiFile, document: Document, span: RustcSpan?, message: String): ApplySuggestionFix? {
//    if (span?.suggested_replacement == null || span.suggestion_applicability == null) return null
//    val textRange = span.toTextRange(document) ?: return null
//    val endElement = file.findElementAt(textRange.endOffset - 1) ?: return null
//    val startElement = file.findElementAt(textRange.startOffset) ?: endElement
//    return ApplySuggestionFix(
//        message,
//        span.suggested_replacement,
//        span.suggestion_applicability,
//        startElement,
//        endElement,
//        textRange
//    )
//}

private fun formatMessage(message: String): String {
    data class Group(val isList: Boolean, val lines: ArrayList<String>)

    val (lastGroup, groups) =
        message.split("\n").fold(
            Pair(null as Group?, ArrayList<Group>())
        ) { (group: Group?, acc: ArrayList<Group>), lineWithPrefix ->
            val (isListItem, line) = if (lineWithPrefix.startsWith("-")) {
                true to lineWithPrefix.substring(2)
            } else {
                false to lineWithPrefix
            }

            when {
                group == null -> Pair(Group(isListItem, arrayListOf(line)), acc)
                group.isList == isListItem -> {
                    group.lines.add(line)
                    Pair(group, acc)
                }
                else -> {
                    acc.add(group)
                    Pair(Group(isListItem, arrayListOf(line)), acc)
                }
            }
        }
    if (lastGroup != null && lastGroup.lines.isNotEmpty()) groups.add(lastGroup)

    return groups.joinToString {
        if (it.isList) "<ul>${it.lines.joinToString("<li>", "<li>")}</ul>"
        else it.lines.joinToString("<br>")
    }
}

