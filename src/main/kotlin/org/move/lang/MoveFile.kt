package org.move.lang

import com.intellij.extapi.psi.PsiFileBase
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.FileViewProvider
import com.intellij.psi.util.PsiTreeUtil
import org.move.lang.core.psi.MoveAddressBlock
import org.move.lang.core.psi.MoveAddressDef
import org.move.lang.core.psi.MoveScriptBlock
import org.move.lang.core.psi.MoveScriptDef

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

//    override fun getStub(): MoveFileStub? = super.getStub() as MoveFileStub?
}

val VirtualFile.isMoveFile: Boolean get() = fileType == MoveFileType
