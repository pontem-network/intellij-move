package org.move.ide.formatter

import com.intellij.formatting.Alignment
import com.intellij.formatting.SpacingBuilder
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.codeStyle.CommonCodeStyleSettings
import org.move.ide.formatter.impl.createSpacingBuilder
import org.move.lang.MvLanguage

data class MvFmtContext(
    val commonSettings: CommonCodeStyleSettings,
    val spacingBuilder: SpacingBuilder,
    val sharedAlignment: Alignment? = null,
) {
    companion object {
        fun create(settings: CodeStyleSettings): MvFmtContext {
            val commonSettings = settings.getCommonSettings(MvLanguage)
            return MvFmtContext(commonSettings, createSpacingBuilder(commonSettings))
        }
    }

}
