/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.move.openapiext

import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import org.move.openapiext.common.isUnitTestMode

fun fullyRefreshDirectory(directory: VirtualFile) {
    VfsUtil.markDirtyAndRefresh(
        false,
        true,
        true,
        directory
    )
}

fun VirtualFile.toPsiFile(project: Project): PsiFile? =
    PsiManager.getInstance(project).findFile(this)

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
