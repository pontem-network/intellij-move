package org.move.lang

import com.intellij.lexer.FlexAdapter

class MoveFlexAdapter : FlexAdapter(_MoveLexer(null))