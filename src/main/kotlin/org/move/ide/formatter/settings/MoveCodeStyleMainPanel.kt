package org.move.ide.formatter.settings

import com.intellij.application.options.TabbedLanguageCodeStylePanel
import com.intellij.psi.codeStyle.CodeStyleSettings
import org.move.lang.MoveLanguage

class MoveCodeStyleMainPanel(currentSettings: CodeStyleSettings, settings: CodeStyleSettings) :
    TabbedLanguageCodeStylePanel(MoveLanguage, currentSettings, settings) {

    override fun initTabs(settings: CodeStyleSettings) {
        addIndentOptionsTab(settings)
//        addSpacesTab(settings)
//        addWrappingAndBracesTab(settings)
//        addBlankLinesTab(settings)
    }
}
