package org.move.lang.core.lexer

import com.intellij.lexer.FlexAdapter
import org.move.lang._MoveLexer

fun createMoveLexer(): MoveLexer = MoveLexer()

class MoveLexer : FlexAdapter(_MoveLexer(null))
