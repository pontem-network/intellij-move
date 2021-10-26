package org.move.lang

import com.intellij.extapi.psi.PsiFileBase
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import org.move.cli.ManifestType
import org.move.cli.findCurrentTomlManifestPath
import org.move.lang.core.psi.MoveAddressBlock
import org.move.lang.core.psi.MoveAddressDef
import org.move.lang.core.psi.MoveScriptBlock
import org.move.lang.core.psi.MoveScriptDef
import org.move.manifest.MoveToml
import org.move.openapiext.parseToml
import org.toml.lang.psi.TomlFile

fun PsiFile.getCorrespondingManifestTomlFile(): Pair<TomlFile, ManifestType>? {
    try {
        val (tomlPath, tomlManifestType) =
            this.virtualFile.toNioPath()
                .let { findCurrentTomlManifestPath(it) } ?: return null
        val tomlFile = parseToml(this.project, tomlPath) ?: return null
        return Pair(tomlFile, tomlManifestType)
    } catch (e: UnsupportedOperationException) {
        return null
    }
}

fun PsiFile.getCorrespondingMoveToml(): MoveToml? {
    val (tomlFile, tomlManifestType) = getCorrespondingManifestTomlFile() ?: return null
    if (tomlManifestType != ManifestType.MOVE_CLI) return null
    return MoveToml.parse(tomlFile)
}

class MoveFile(fileViewProvider: FileViewProvider) : PsiFileBase(fileViewProvider, MoveLanguage) {
    override fun getFileType(): FileType = MoveFileType

    fun addressBlocks(): List<MoveAddressBlock> {
        val defs = PsiTreeUtil.getChildrenOfTypeAsList(this, MoveAddressDef::class.java)
        return defs.mapNotNull { it.addressBlock }.toList()
    }

    fun scriptBlocks(): List<MoveScriptBlock> {
        val defs = PsiTreeUtil.getChildrenOfTypeAsList(this, MoveScriptDef::class.java)
        return defs.mapNotNull { it.scriptBlock }.toList()
    }
}

val VirtualFile.isMoveFile: Boolean get() = fileType == MoveFileType
