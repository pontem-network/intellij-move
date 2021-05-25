package org.move.lang.core.completion

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.PrioritizedLookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.util.ProcessingContext
import org.move.lang.core.psi.ext.isErrorElement
import org.move.lang.core.psi.ext.isWhitespace
import org.move.lang.core.psi.ext.rightSiblings

class KeywordCompletionProvider(
    private vararg val keywords: String,
) : CompletionProvider<CompletionParameters>() {

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet,
    ) {
        for (keyword in keywords) {
            var element =
                LookupElementBuilder.create(keyword).bold()
            val nextSibling = parameters.position.parent
                .rightSiblings
                .filter { !it.isWhitespace() && !it.isErrorElement() }
                .firstOrNull()?.firstChild

            element = element.withInsertHandler { ctx, _ ->
                if (nextSibling == null || !ctx.alreadyHasSpace) ctx.addSuffix(" ")
            }
            result.addElement(PrioritizedLookupElement.withPriority(element, KEYWORD_PRIORITY))
        }
    }
}

//private fun addInsertionHandler(
//    keyword: String,
//    builder: LookupElementBuilder,
//    parameters: CompletionParameters,
//): LookupElementBuilder {
//    val nextSibling = parameters.position.parent.nextSibling
//    if (nextSibling.elementType != TokenType.WHITE_SPACE) {
//        return builder.withInsertHandler { ctx, _ -> ctx.addSuffix(" ") }
//    }
//    return builder
////    val suffix = when (keyword) {
////        "script" -> {
////            val nextSibling = parameters.position.parent.nextSibling
////            if (nextSibling.elementType == TokenType.WHITE_SPACE)
////                ""
////            else
////                " "
////        }
////        else -> " "
////    }
////    return builder.withInsertHandler { ctx, _ -> ctx.addSuffix(suffix) }
//}
