package org.move.ide.formatter.settings

import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.codeStyle.CustomCodeStyleSettings

class MoveCodeStyleSettings(container: CodeStyleSettings) :
    CustomCodeStyleSettings(MoveCodeStyleSettings::class.java.simpleName, container)
