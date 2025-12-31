package org.move.ide.annotator.externalLinter

import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.codeInsight.daemon.impl.HighlightInfoType
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.CheckedDisposable
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
import org.apache.commons.text.StringEscapeUtils
import org.jetbrains.annotations.Nls
import org.move.cli.externalLinter.RsExternalLinterWidget
import org.move.cli.externalLinter.externalLinterSettings
import org.move.cli.runConfigurations.endless.Endless
import org.move.cli.runConfigurations.endless.EndlessExternalLinterArgs
import org.move.cli.runConfigurations.endless.isCompilerJsonOutputEnabled
import org.move.ide.annotator.externalLinter.RsExternalLinterFilteredMessage.Companion.filterMessage
import org.move.ide.annotator.externalLinter.RsExternalLinterUtils.EXTERNAL_LINTER_TEST_MESSAGE
import org.move.ide.notifications.logOrShowBalloon
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
    const val EXTERNAL_LINTER_TEST_MESSAGE: String = "MvExternalLint"

    /**
     * Returns (and caches if absent) lazily computed messages from external linter.
     *
     * Note: before applying this result you need to check that the PSI modification stamp of current project has not
     * changed after calling this method.
     *
     * @see PsiModificationTracker.MODIFICATION_COUNT
     */
    fun checkLazily(
        endlessCli: Endless,
        project: Project,
        workingDirectory: Path,
        linterArgs: EndlessExternalLinterArgs
    ): Lazy<RsExternalLinterResult?> {
        checkReadAccessAllowed()
        return externalLinterLazyResultCache.getOrPut(
            project,
            Key(endlessCli, workingDirectory, linterArgs)
        ) {
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
                checkWrapped(endlessCli, project, linterArgs)
            }
        }
    }

    private fun checkWrapped(
        endlessCli: Endless,
        project: Project,
        linterArgs: EndlessExternalLinterArgs
    ): RsExternalLinterResult? {
        val widget = WriteAction.computeAndWait<RsExternalLinterWidget?, Throwable> {
            saveAllDocumentsAsTheyAre()
            val statusBar = WindowManager.getInstance().getStatusBar(project)
            statusBar?.getWidget(RsExternalLinterWidget.ID) as? RsExternalLinterWidget
        }

        val future = CompletableFuture<RsExternalLinterResult?>()
        val task =
            object: Task.Backgroundable(
                project,
                "Analyzing project with ${linterArgs.linter.title}...",
                true
            ) {

                override fun run(indicator: ProgressIndicator) {
                    widget?.inProgress = true
                    future.complete(check(project, endlessCli, linterArgs))
                }

                override fun onFinished() {
                    widget?.inProgress = false
                }
            }
        ProgressManager.getInstance().runProcessWithProgressAsynchronously(task, EmptyProgressIndicator())
        return future.get()
    }

    private fun check(
        project: Project,
        endless: Endless,
        linterArgs: EndlessExternalLinterArgs
    ): RsExternalLinterResult? {
        ProgressManager.checkCanceled()
        val started = Instant.now()
        val output = endless.checkProject(project, linterArgs)
            .unwrapOrElse { e ->
                LOG.error(e)
                return null
            }
        val finish = Instant.now()
        ProgressManager.checkCanceled()
        if (output.isCancelled) return null
        return RsExternalLinterResult(
            output.stderrLines + output.stdoutLines,
            Duration.between(started, finish).toMillis()
        )
    }

    private data class Key(
        val endlessCli: Endless,
        val workingDirectory: Path,
        val args: EndlessExternalLinterArgs
    )

    private val externalLinterLazyResultCache =
        ProjectCache<Key, Lazy<RsExternalLinterResult?>>("externalLinterLazyResultCache") {
            PsiModificationTracker.MODIFICATION_COUNT
        }
}

fun MessageBus.createDisposableOnAnyPsiChange(): CheckedDisposable {
    val disposable = Disposer.newCheckedDisposable("Dispose on PSI change")
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
) {
    val document = file.viewProvider.document
        ?: error("Can't find document for $file in external linter")

    val project = file.project
    val skipIdeErrors = project.externalLinterSettings.skipErrorsKnownToIde

    val compilerErrors =
        if (project.isCompilerJsonOutputEnabled) {
            annotationResult.jsonCompilerErrors
        } else {
            annotationResult.humanCompilerErrors
                .mapNotNull { it.toJsonError(file, document) }
        }
            .mapNotNull { filterMessage(file, it, skipIdeErrors) }
            .distinct()

    for (compilerError in compilerErrors) {
        val highlightBuilder = HighlightInfo
            .newHighlightInfo(convertSeverity(compilerError.severity))
            .severity(compilerError.severity)
            // We can't control what messages cargo generates, so we can't test them well.
            // Let's use the special message for tests to distinguish annotation from external linter
            .description(if (isUnitTestMode) EXTERNAL_LINTER_TEST_MESSAGE else compilerError.message)
            .escapedToolTip(compilerError.htmlTooltip)
            .range(compilerError.textRange)
            .needsUpdateOnTyping(true)

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

class RsExternalLinterResult(
    val outputLines: List<String>,
    val executionTime: Long
) {
    val humanCompilerErrors: List<HumanEndlessCompilerError> get() = parseHumanCompilerErrors(outputLines)
    val jsonCompilerErrors: List<JsonEndlessCompilerError> get() = parseJsonCompilerErrors(outputLines)
}

private data class RsExternalLinterFilteredMessage(
    val severity: HighlightSeverity,
    val textRange: TextRange,
    @Nls val message: String,
    @Nls val htmlTooltip: String,
) {
    companion object {
        private val LOG = logger<RsExternalLinterFilteredMessage>()

        fun filterMessage(
            file: PsiFile,
            compilerError: JsonEndlessCompilerError,
            skipErrorsKnownToIde: Boolean,
        ): RsExternalLinterFilteredMessage? {
            val highlightSeverity = when (compilerError.severity) {
                "Bug", "Error" -> HighlightSeverity.ERROR
                "Warning", "Warning [lint]" -> HighlightSeverity.WEAK_WARNING
                else -> HighlightSeverity.INFORMATION
            }

            // drop syntax errors
            if (compilerError.message == "unexpected token") return null

            if (skipErrorsKnownToIde) {
                val errorsToIgnore = listOf(
                    // name resolution errors
                    "unbound variable", "undeclared",
                    // type errors
                    "incompatible types", "which expects a value of type", "which expects argument of type",
                    // too many arguments
                    "too many arguments", "the function takes",
                    // missing fields
                    "too few arguments", "missing fields",
                    // unused imports
                    "unused alias",
                )
                if (errorsToIgnore.any { it in compilerError.message }) {
                    LOG.logOrShowBalloon("ignore external linter error", compilerError.toTestString())
                    return null
                }
            }

            // skip global errors
            val primaryLabel = compilerError.labels.find { it.style == "Primary" } ?: return null

            // skip labels from other files
            val spanFilePath = PathUtil.toSystemIndependentName(primaryLabel.file_id)
            if (!file.virtualFile.path.endsWith(spanFilePath)) return null

            val primaryTextRange = primaryLabel.range.toTextRange()

            val tooltip = buildString {
                append(formatMessage(StringEscapeUtils.escapeHtml4(compilerError.message)).escapeUrls())
                with(mutableListOf<String>()) {
//                    if (span.label != null && !compilerError.text.startsWith(span.label)) {
//                        add(StringEscapeUtils.escapeHtml4(span.label))
//                    }

//                    message.children
//                        .filter { it.message.isNotBlank() }
//                        .map { "${it.level.capitalized()}: ${StringEscapeUtils.escapeHtml4(it.message)}" }
//                        .forEach { add(it) }

                    append(joinToString(prefix = "<br>", separator = "<br>") { formatMessage(it) }.escapeUrls())
                }
            }

            return RsExternalLinterFilteredMessage(
                highlightSeverity,
                primaryTextRange,
                compilerError.message.capitalized(),
                tooltip,
//                compilerError.code?.let {  }
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

