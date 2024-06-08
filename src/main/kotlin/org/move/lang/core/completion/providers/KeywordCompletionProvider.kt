package org.move.lang.core.completion.providers

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.editor.EditorModificationUtil
import com.intellij.openapi.project.Project
import com.intellij.psi.util.elementType
import com.intellij.util.ProcessingContext
import org.move.lang.MvElementTypes.L_BRACE
import org.move.lang.core.completion.*
import org.move.lang.core.psi.ext.isErrorElement
import org.move.lang.core.psi.ext.isWhitespace
import org.move.lang.core.psi.ext.rightSiblings

class KeywordCompletionProvider(
    private val fillCompletions: (Project) -> List<String>
):
    CompletionProvider<CompletionParameters>() {

    constructor(vararg keywords: String): this({ keywords.toList() })

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet,
    ) {
        val project = parameters.position.project
        val keywords = fillCompletions(project)
        for (keyword in keywords) {
            var builder = LookupElementBuilder.create(keyword).bold()
            builder = addInsertionHandler(keyword, builder, parameters)
            result.addElement(builder.withPriority(KEYWORD_PRIORITY))
        }
    }
}

private fun addInsertionHandler(
    keyword: String,
    builder: LookupElementBuilder,
    parameters: CompletionParameters
): LookupElementBuilder {
    val posParent = parameters.position.parent
    val posRightSiblings =
        posParent.rightSiblings.filter { !it.isWhitespace() && !it.isErrorElement() }
    val posParentNextSibling = posRightSiblings.firstOrNull()?.firstChild

    return builder.withInsertHandler { ctx, _ ->
        val elemSibling = parameters.position.nextSibling
        val suffix = when {
            keyword == "acquires" && posParentNextSibling.elementType == L_BRACE -> {
                when {
                    ctx.nextCharIs(' ', 0) && ctx.nextCharIs(' ', 1) -> {
                        EditorModificationUtil.moveCaretRelatively(ctx.editor, 1)
                        ""
                    }
                    ctx.nextCharIs(' ') -> {
                        " "
                    }
                    else -> {
                        EditorModificationUtil.moveCaretRelatively(ctx.editor, -1)
                        "  "
                    }
                }
            }
            elemSibling != null && elemSibling.isWhitespace() -> ""
            posParentNextSibling == null || !ctx.alreadyHasSpace -> " "
            else -> ""
        }
        ctx.addSuffix(suffix)
    }
}
