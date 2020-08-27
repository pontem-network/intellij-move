package org.move.ide.formatter

import com.intellij.formatting.Alignment
import com.intellij.formatting.SpacingBuilder
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.codeStyle.CommonCodeStyleSettings
import org.move.ide.formatter.impl.createSpacingBuilder
import org.move.lang.MoveLanguage

data class MoveFmtContext private constructor(
    val commonSettings: CommonCodeStyleSettings,
    val spacingBuilder: SpacingBuilder,
    val sharedAlignment: Alignment? = null
) {
    companion object {
        fun create(settings: CodeStyleSettings): MoveFmtContext {
            val commonSettings = settings.getCommonSettings(MoveLanguage)
            return MoveFmtContext(commonSettings, createSpacingBuilder(commonSettings))
        }
    }

}