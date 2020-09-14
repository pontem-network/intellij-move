package org.move.lang

import com.intellij.lexer.FlexAdapter
import com.intellij.lexer.RestartableLexer
import com.intellij.lexer.TokenIterator

@Suppress("UnstableApiUsage")
class MoveLexer : FlexAdapter(_MoveLexer(null)),
                  RestartableLexer {

    override fun getStartState(): Int = state

    override fun isRestartableState(state: Int): Boolean = false

    override fun start(
        buffer: CharSequence,
        startOffset: Int,
        endOffset: Int,
        initialState: Int,
        tokenIterator: TokenIterator?,
    ) =
        super.start(buffer, startOffset, endOffset, initialState)
}