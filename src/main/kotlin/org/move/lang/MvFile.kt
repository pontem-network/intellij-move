package org.move.lang

import com.intellij.extapi.psi.PsiFileBase
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import org.move.cli.MoveProject
import org.move.cli.MvConstants
import org.move.cli.moveProjects
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.childrenOfType
import org.move.lang.core.psi.ext.modules
import org.move.openapiext.resolveAbsPath
import org.move.openapiext.toPsiFile
import org.move.stdext.chain
import org.toml.lang.psi.TomlFileType
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

//fun PsiFile.getCorrespondingMoveTomlFile(): TomlFile? {
//    return project.moveProjects.findMvProjectForPsiFile(this)?.moveToml?.tomlFile
//    val moveTomlPath =
//        addressesService.findMoveTomlPathForFile(this.originalFile.virtualFile)
//            ?: return null
//    val moveTomlPath = this.toNioPathOrNull()?.let { findMoveTomlPath(it) } ?: return null
//    return parseToml(this.project, moveTomlPath)
//}

//fun PsiFile.getCorrespondingMoveToml(): MoveToml? {
//    val tomlFile = getCorrespondingMoveTomlFile() ?: return null
//    return MoveToml.fromTomlFile(tomlFile)
//}

//fun PsiDirectory.moveProject(): MoveProject? {
//    project.moveProjectsService.findMoveProjectForPsiFile()
//}
val PsiElement.moveProject: MoveProject?
    get() = project.moveProjects.findProjectForPsiElement(this)

fun VirtualFile.toNioPathOrNull(): Path? {
    try {
        return this.toNioPath()
    } catch (e: UnsupportedOperationException) {
        return null
    }
}

fun PsiFile.toNioPathOrNull(): Path? {
    return this.originalFile.virtualFile.toNioPathOrNull()
}

class MvFile(fileViewProvider: FileViewProvider) : PsiFileBase(fileViewProvider, MvLanguage) {
    override fun getFileType(): FileType = MoveFileType

    fun addressBlocks(): List<MvAddressBlock> {
        val defs = PsiTreeUtil.getChildrenOfTypeAsList(this, MvAddressDef::class.java)
        return defs.mapNotNull { it.addressBlock }.toList()
    }

    fun scriptBlocks(): List<MvScriptBlock> {
        val defs = PsiTreeUtil.getChildrenOfTypeAsList(this, MvScript::class.java)
        return defs.mapNotNull { it.scriptBlock }.toList()
    }
}

val VirtualFile.isMoveFile: Boolean get() = fileType == MoveFileType

val VirtualFile.isMoveTomlManifestFile: Boolean get() = fileType == TomlFileType && name == "Move.toml"

fun VirtualFile.toMvFile(project: Project): MvFile? = this.toPsiFile(project) as? MvFile

fun MvFile.modules(): Sequence<MvModule> {
    return this.childrenOfType<MvModule>()
        .chain(this.childrenOfType<MvAddressDef>().flatMap { it.modules() })
}
