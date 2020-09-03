package org.move.ide.formatter.impl

import org.move.ide.formatter.MoveAlignmentStrategy
import org.move.ide.formatter.MoveFormatterBlock
import org.move.lang.MoveElementTypes.*

fun MoveFormatterBlock.getAlignmentStrategy(): MoveAlignmentStrategy = when (node.elementType) {
    FUNCTION_PARAMETER_LIST, CALL_ARGUMENTS ->
        MoveAlignmentStrategy
            .shared()
            .alignUnlessBlockDelim()
            .alignIf(ctx.commonSettings.ALIGN_MULTILINE_PARAMETERS)
    TYPE_PARAMETER_LIST ->
        MoveAlignmentStrategy
            .wrap()
            .alignIf(TYPE_PARAMETER)
    else -> MoveAlignmentStrategy.NullStrategy

}

fun MoveAlignmentStrategy.alignUnlessBlockDelim(): MoveAlignmentStrategy =
    alignIf { c, p, _ -> !c.isDelimiterOfCurrentBlock(p) }