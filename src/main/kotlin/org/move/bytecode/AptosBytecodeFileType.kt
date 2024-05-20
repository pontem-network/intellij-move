package org.move.bytecode

import com.intellij.openapi.fileTypes.FileType
import org.move.ide.MoveIcons

object AptosBytecodeFileType: FileType {
    override fun getIcon() = MoveIcons.MV_LOGO
    override fun getName() = "APTOS_BYTECODE"
    override fun getDefaultExtension() = "mv"
    override fun getDescription() = "Aptos Move bytecode"
    override fun getDisplayName() = "Aptos Move bytecode"
    override fun isBinary(): Boolean = true
}