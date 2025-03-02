package org.move.lang

import com.intellij.extapi.psi.PsiFileBase
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager.getProjectPsiDependentCache
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.psi.util.PsiTreeUtil
import org.move.cli.MoveProject
import org.move.cli.MvConstants
import org.move.cli.moveProjectsService
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.ancestorOrSelf
import org.move.lang.core.psi.ext.childrenOfType
import org.move.lang.core.psi.ext.modules
import org.move.lang.core.resolve.PsiFileCachedValueProvider
import org.move.lang.core.resolve.getResults
import org.move.openapiext.resolveAbsPath
import org.move.openapiext.toPsiFile
import org.move.stdext.chain
import org.move.utils.fileCacheResult
import org.toml.lang.psi.TomlFile
import java.nio.file.Path

fun findMoveTomlPath(currentFilePath: Path): Path? {
    var dir = currentFilePath.parent
    while (dir != null) {
        val moveTomlPath = dir.resolveAbsPath(MvConstants.MANIFEST_FILE)
        if (moveTomlPath != null) {
            return moveTomlPath
        }
        dir = dir.parent
    }
    return null
}

// requires ReadAccess
val PsiElement.moveProject: MoveProject?
    get() {
        return project.moveProjectsService.findMoveProjectForPsiElement(this)
    }

fun VirtualFile.hasChild(name: String) = this.findChild(name) != null

fun VirtualFile.toNioPathOrNull() = fileSystem.getNioPath(this)
//fun VirtualFile.toNioPathOrNull(): Path? {
//    try {
//        return this.toNioPath()
//    } catch (e: UnsupportedOperationException) {
//        return null
//    }
//}

fun PsiFile.toNioPathOrNull(): Path? {
    return this.originalFile.virtualFile?.toNioPathOrNull()
}

abstract class MoveFileBase(fileViewProvider: FileViewProvider): PsiFileBase(fileViewProvider, MoveLanguage) {
    override fun getFileType(): FileType = MoveFileType
}

class FileModules(override val file: MoveFile): PsiFileCachedValueProvider<List<MvModule>> {
    override fun compute(): CachedValueProvider.Result<List<MvModule>> {
        val children = file.children
        val modules = buildList(children.size) {
            for (child in children) {
                when (child) {
                    is MvModule -> add(child)
                    is MvAddressDef -> {
                        addAll(child.modules())
                    }
                }
            }
        }
        return file.virtualFile.fileCacheResult(modules)
    }
}

class MoveFile(fileViewProvider: FileViewProvider): MoveFileBase(fileViewProvider) {

    fun scripts(): List<MvScript> = this.childrenOfType<MvScript>()

    fun modules(): List<MvModule> = FileModules(this).getResults()

    fun moduleSpecs(): List<MvModuleSpec> = this.childrenOfType()
}

val VirtualFile.isMoveFile: Boolean get() = fileType == MoveFileType

val VirtualFile.isMoveTomlManifestFile: Boolean get() = name == "Move.toml"

fun VirtualFile.toMoveFile(project: Project): MoveFile? = this.toPsiFile(project) as? MoveFile

fun VirtualFile.toTomlFile(project: Project): TomlFile? = this.toPsiFile(project) as? TomlFile

inline fun <reified T: PsiElement> PsiFile.elementAtOffset(offset: Int): T? =
    this.findElementAt(offset)?.ancestorOrSelf<T>()
