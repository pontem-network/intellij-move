package org.move.lang

import com.intellij.openapi.fileTypes.LanguageFileType
import org.move.ide.MvIcons
import javax.swing.Icon

object MoveFileType : LanguageFileType(MvLanguage) {
    override fun getIcon() = MvIcons.MOVE
    override fun getName() = "Move"
    override fun getDefaultExtension() = "move"
    override fun getDescription() = "Move Language file"
}
