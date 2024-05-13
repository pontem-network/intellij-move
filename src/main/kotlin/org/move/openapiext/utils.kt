/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.move.openapiext

import com.intellij.execution.Platform
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.Experiments
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.Service.Level.PROJECT
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.impl.TrailingSpacesStripper
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.AdditionalLibraryRootsProvider
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.SyntheticLibrary
import com.intellij.openapi.util.Computable
import com.intellij.openapi.util.JDOMUtil
import com.intellij.openapi.util.NlsContexts
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.*
import com.intellij.psi.impl.PsiDocumentManagerBase
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.StubIndex
import com.intellij.psi.stubs.StubIndexKey
import com.intellij.util.concurrency.AppExecutorUtil
import org.jdom.Element
import org.move.lang.toNioPathOrNull
import org.move.openapiext.common.isHeadlessEnvironment
import org.move.openapiext.common.isUnitTestMode
import org.rust.ide.annotator.RsExternalLinterPass
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.Callable

fun <T> Project.runWriteCommandAction(command: () -> T): T {
    return WriteCommandAction.runWriteCommandAction(this, Computable<T> { command() })
}

fun fullyRefreshDirectory(directory: VirtualFile) {
    VfsUtil.markDirtyAndRefresh(
        false,
        true,
        true,
        directory
    )
}

val VirtualFile.pathAsPath: Path get() = Paths.get(path)

fun VirtualFile.toPsiFile(project: Project): PsiFile? =
    PsiManager.getInstance(project).findFile(this)

fun VirtualFile.toPsiDirectory(project: Project): PsiDirectory? =
    PsiManager.getInstance(project).findDirectory(this)

val PsiFile.document: Document?
    get() = PsiDocumentManager.getInstance(project).getDocument(this)

fun Document.getOffsetPosition(offset: Int): Pair<Int, Int> {
    val lineNumber = this.getLineNumber(offset) + 1
    val columnNumber = offset - this.getLineStartOffset(lineNumber - 1)
    return Pair(lineNumber, columnNumber)
}

fun saveAllDocuments() = FileDocumentManager.getInstance().saveAllDocuments()

/**
 * Calling of [saveAllDocuments] uses [TrailingSpacesStripper] to format all unsaved documents.
 *
 * In case of [RsExternalLinterPass] it backfires:
 * 1. Calling [TrailingSpacesStripper.strip] on *every* file change.
 * 2. Double run of external linter, because [TrailingSpacesStripper.strip] generates new "PSI change" events.
 *
 * This function saves all documents "as they are" (see [FileDocumentManager.saveDocumentAsIs]), but also fires that
 * these documents should be stripped later (when [saveAllDocuments] is called).
 */
fun saveAllDocumentsAsTheyAre(reformatLater: Boolean = true) {
    val documentManager = FileDocumentManager.getInstance()
//    val rustfmtWatcher = RustfmtWatcher.getInstance()
//    rustfmtWatcher.withoutReformatting {
    for (document in documentManager.unsavedDocuments) {
        documentManager.saveDocumentAsIs(document)
//            documentManager.stripDocumentLater(document)
//        if (reformatLater) rustfmtWatcher.reformatDocumentLater(document)
    }
//    }
}

inline fun testAssert(action: () -> Boolean, lazyMessage: () -> Any) {
    if (isUnitTestMode && !action()) {
        val message = lazyMessage()
        throw AssertionError(message)
    }
}

val DataContext.psiFile: PsiFile?
    get() = getData(CommonDataKeys.PSI_FILE)

val DataContext.editor: Editor?
    get() = getData(CommonDataKeys.EDITOR)

val DataContext.project: Project?
    get() = getData(CommonDataKeys.PROJECT)

fun checkWriteAccessAllowed() {
    check(ApplicationManager.getApplication().isWriteAccessAllowed) {
        "Needs write action"
    }
}

fun checkWriteAccessNotAllowed() {
    check(!ApplicationManager.getApplication().isWriteAccessAllowed)
}

fun isReadAccessAllowed() = ApplicationManager.getApplication().isReadAccessAllowed

fun checkReadAccessAllowed() {
    check(ApplicationManager.getApplication().isReadAccessAllowed) {
        "Needs read action"
    }
}

fun checkReadAccessNotAllowed() {
    check(!ApplicationManager.getApplication().isReadAccessAllowed)
}

val Project.modules: Collection<Module>
    get() = ModuleManager.getInstance(this).modules.toList()

val Project.contentRoots: Sequence<VirtualFile>
    get() = this.modules.asSequence()
        .flatMap { ModuleRootManager.getInstance(it).contentRoots.asSequence() }

val Project.syntheticLibraries: Collection<SyntheticLibrary>
    get() {
        val libraries = AdditionalLibraryRootsProvider.EP_NAME
            .extensionList
            .flatMap { it.getAdditionalProjectLibraries(this) }
        return libraries
    }

val Project.rootDir: VirtualFile? get() = contentRoots.firstOrNull()

val Project.rootPath: Path? get() = contentRoots.firstOrNull()?.toNioPathOrNull()

val Project.contentRoot: VirtualFile? get() = contentRoots.firstOrNull()

fun Element.toXmlString() = JDOMUtil.writeElement(this)

fun <T> Project.computeWithCancelableProgress(
    @NlsContexts.ProgressTitle title: String,
    supplier: () -> T
): T {
    if (isUnitTestMode) {
        return supplier()
    }
    return ProgressManager.getInstance()
        .runProcessWithProgressSynchronously<T, Exception>(supplier, title, true, this)
}

fun checkIsDispatchThread() {
    check(ApplicationManager.getApplication().isDispatchThread) {
        "Should be invoked on the Swing dispatch thread"
    }
}

fun checkIsBackgroundThread() {
    check(!ApplicationManager.getApplication().isDispatchThread) {
        "Long running operation invoked on UI thread"
    }
}

fun isFeatureEnabled(featureId: String): Boolean {
    // Hack to pass values of experimental features in headless IDE run
    // Should help to configure IDE-based tools like Qodana
    if (isHeadlessEnvironment) {
        val value = System.getProperty(featureId)?.toBooleanStrictOrNull()
        if (value != null) return value
    }

    return Experiments.getInstance().isFeatureEnabled(featureId)
}

/** Intended to be invoked from EDT */
inline fun <R> Project.nonBlocking(crossinline block: () -> R, crossinline uiContinuation: (R) -> Unit) {
    if (isUnitTestMode) {
        val result = block()
        uiContinuation(result)
    } else {
        ReadAction.nonBlocking(Callable {
            block()
        })
            .inSmartMode(this)
            .expireWith(this.rootDisposable)
            .finishOnUiThread(ModalityState.current()) { result ->
                uiContinuation(result)
            }.submit(AppExecutorUtil.getAppExecutorService())
    }
}

@Service(PROJECT)
class RootPluginDisposable: Disposable {
    override fun dispose() {}
}

val Project.rootDisposable get() = this.service<RootPluginDisposable>()

fun checkCommitIsNotInProgress(project: Project) {
    val app = ApplicationManager.getApplication()
    if ((app.isUnitTestMode || app.isInternal) && app.isDispatchThread) {
        if ((PsiDocumentManager.getInstance(project) as PsiDocumentManagerBase).isCommitInProgress) {
            error("Accessing indices during PSI event processing can lead to typing performance issues")
        }
    }
}

inline fun <Key: Any, reified Psi: PsiElement> getElements(
    indexKey: StubIndexKey<Key, Psi>,
    key: Key,
    project: Project,
    scope: GlobalSearchScope?
): Collection<Psi> =
    StubIndex.getElements(indexKey, key, project, scope, Psi::class.java)

fun joinPath(segments: Array<String>) =
    segments.joinTo(StringBuilder(), Platform.current().fileSeparator.toString()).toString()