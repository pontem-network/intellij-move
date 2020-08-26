package org.move.lang

import com.intellij.openapi.fileTypes.LanguageFileType
import org.move.ide.MvIcons
import javax.swing.Icon

object MoveFileType : LanguageFileType(MoveLanguage) {
    override fun getIcon(): Icon? = MvIcons.MOVE
    override fun getName(): String = "Move"
    override fun getDefaultExtension(): String = "move"
    override fun getDescription(): String = "Move Language file"
}