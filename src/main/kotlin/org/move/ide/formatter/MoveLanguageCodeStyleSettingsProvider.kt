package org.move.ide.formatter

import com.intellij.lang.Language
import com.intellij.psi.codeStyle.LanguageCodeStyleSettingsProvider
import org.move.lang.MoveLanguage

class MoveLanguageCodeStyleSettingsProvider: LanguageCodeStyleSettingsProvider() {
    override fun getLanguage(): Language = MoveLanguage

    override fun getCodeSample(settingsType: SettingsType): String = ""
}