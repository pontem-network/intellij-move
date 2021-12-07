package org.move.ide.formatter

import com.intellij.lang.Language
import com.intellij.psi.codeStyle.LanguageCodeStyleSettingsProvider
import org.move.lang.MvLanguage

class MvLanguageCodeStyleSettingsProvider: LanguageCodeStyleSettingsProvider() {
    override fun getLanguage(): Language = MvLanguage

    override fun getCodeSample(settingsType: SettingsType): String = ""
}
