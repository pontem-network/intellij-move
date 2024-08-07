package org.move.ide.formatter

import com.intellij.formatting.Alignment
import com.intellij.formatting.SpacingBuilder
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.codeStyle.CommonCodeStyleSettings
import org.move.ide.formatter.impl.createSpacingBuilder
import org.move.lang.MoveLanguage

data class MvFmtContext(
    val commonSettings: CommonCodeStyleSettings,
    val spacingBuilder: SpacingBuilder,
    /**
     * Stores shared alignment object, e.g. for function declarations's parameters, return type & where clause.
     */
    val sharedAlignment: Alignment? = null,

    /**
     * Determine whether we have spotted opening delimiter during
     * construction of a _flat block_'s sub blocks list.
     *
     * We only care about opening delimiters (`(`, `[`, `{`, `<`, `|`) here,
     * because none of flat blocks has any children after block part (apart
     * from closing delimiter, which we have to handle separately anyways).
     *
     * @see isFlatBlock
     */
    val metLBrace: Boolean = false
) {
    companion object {
        fun create(settings: CodeStyleSettings): MvFmtContext {
            val commonSettings = settings.getCommonSettings(MoveLanguage)
            return MvFmtContext(
                commonSettings = commonSettings,
                spacingBuilder = createSpacingBuilder(commonSettings),
            )
        }
    }
}
