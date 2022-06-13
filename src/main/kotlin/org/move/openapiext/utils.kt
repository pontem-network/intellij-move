/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.move.openapiext

import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.Experiments
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.util.Computable
import com.intellij.openapi.util.JDOMUtil
import com.intellij.openapi.util.NlsContexts
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import org.jdom.Element
import org.jdom.input.SAXBuilder
import org.move.lang.toNioPathOrNull
import org.move.openapiext.common.isHeadlessEnvironment
import org.move.openapiext.common.isUnitTestMode
import java.nio.file.Path
import java.nio.file.Paths

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

fun saveAllDocuments() = FileDocumentManager.getInstance().saveAllDocuments()

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

val Project.root: Path? get() = contentRoots.firstOrNull()?.toNioPathOrNull()

val Project.contentRoot: VirtualFile? get() = contentRoots.firstOrNull()

fun Element.toXmlString() = JDOMUtil.writeElement(this)
fun elementFromXmlString(xml: String): org.jdom.Element =
    SAXBuilder().build(xml.byteInputStream()).rootElement

fun <T> Project.computeWithCancelableProgress(
    @Suppress("UnstableApiUsage") @NlsContexts.ProgressTitle title: String,
    supplier: () -> T
): T {
    if (isUnitTestMode) {
        return supplier()
    }
    return ProgressManager.getInstance().runProcessWithProgressSynchronously<T, Exception>(supplier, title, true, this)
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
