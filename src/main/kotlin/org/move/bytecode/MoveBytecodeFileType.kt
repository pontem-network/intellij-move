package org.move.bytecode

import com.intellij.openapi.fileTypes.FileType
import org.move.ide.MoveIcons

object MoveBytecodeFileType: FileType {
    override fun getIcon() = MoveIcons.MV_LOGO
    override fun getName() = "MOVE_BYTECODE"
    override fun getDefaultExtension() = "mv"
    override fun getDescription() = "Endless Move bytecode"
    override fun getDisplayName() = "Endless Move bytecode"
    override fun isBinary(): Boolean = true
}