package org.move.lang

import com.intellij.icons.AllIcons
import com.intellij.openapi.fileTypes.LanguageFileType
import javax.swing.Icon

object MoveFileType : LanguageFileType(MoveLanguage) {
    override fun getIcon(): Icon? = AllIcons.Plugins.ModifierJBLogo
    override fun getName(): String = "Move"
    override fun getDefaultExtension(): String = "move"
    override fun getDescription(): String = "Move Language file"
}