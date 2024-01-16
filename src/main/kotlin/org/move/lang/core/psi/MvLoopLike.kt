package org.move.lang.core.psi

interface MvLoopLike: MvElement {
    val codeBlock: MvCodeBlock?
    val inlineBlock: MvInlineBlock?
}