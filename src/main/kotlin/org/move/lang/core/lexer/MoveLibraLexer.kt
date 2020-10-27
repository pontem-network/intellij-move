package org.move.lang.core.lexer

import com.intellij.lexer.LexerBase

enum class LexerState(private val value: Int) {
    OUTER(0),
    CODE(1),
    SPEC(2);

    fun asInt(): Int = value

    companion object {
        private val map = values().associateBy(LexerState::value)
        fun fromInt(type: Int) = map[type] ?: error("No such state")
    }
}

//class MoveLibraLexer(
//    var text: String,
//    var currentPos: Int,
//    var state: LexerState,
//) : LexerBase() {
//
//    override fun start(buffer: CharSequence, startOffset: Int, endOffset: Int, initialState: Int) {
//        this.text = buffer.toString()
//        this.currentPos = 0
//        this.state = LexerState.fromInt(initialState)
//    }
//
//    override fun getState(): Int = state.asInt()
//
//    override fun getTokenStart(): Int {
//        TODO("Not yet implemented")
//    }
//
//    override fun getTokenEnd(): Int {
//        TODO("Not yet implemented")
//    }
//
//    override fun advance() {
//        TODO("Not yet implemented")
//    }
//}