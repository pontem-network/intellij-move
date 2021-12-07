package org.move.lang

import com.intellij.lang.Language

object MvLanguage : Language("Move") {
    override fun isCaseSensitive() = true
    override fun getDisplayName() = "Move"
}
