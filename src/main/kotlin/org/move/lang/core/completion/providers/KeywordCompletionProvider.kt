package org.move.lang.core.completion.providers

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.editor.EditorModificationUtil
import com.intellij.openapi.project.Project
import com.intellij.psi.util.elementType
import com.intellij.util.ProcessingContext
import org.move.lang.MvElementTypes.L_BRACE
import org.move.lang.core.completion.*
import org.move.lang.core.psi.MvFunction
import org.move.lang.core.psi.ext.*
import org.move.lang.core.psi.isNative

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
            result.addElement(createKeywordLookupElement(keyword, parameters))
        }
    }
}

class FunctionModifierCompletionProvider: CompletionProvider<CompletionParameters>() {
    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val ident = parameters.position
        val function = ident.parent as? MvFunction ?: return

        val keywords = buildList {
            if (function.visibilityModifier == null) add("public")
            if (!function.isEntry) add("entry")
            if (!function.isNative) add("native")
            if (!function.isInline) add("inline")
        }
        for (keyword in keywords) {
            result.addElement(createKeywordLookupElement(keyword, parameters))
        }
    }
}

private fun createKeywordLookupElement(keyword: String, parameters: CompletionParameters): LookupElement {
    return LookupElementBuilder
        .create(keyword)
        .bold()
        .withInsertHandler(KeywordInsertionHandler(keyword, parameters))
        .withPriority(KEYWORD_PRIORITY)
}

private class KeywordInsertionHandler(
    val keyword: String,
    val parameters: CompletionParameters,
): InsertHandler<LookupElement> {
    override fun handleInsert(context: InsertionContext, item: LookupElement) {
        val posParent = parameters.position.parent
        val posRightSiblings =
            posParent.rightSiblings.filter { !it.isWhitespace() && !it.isErrorElement() }
        val posParentNextSibling = posRightSiblings.firstOrNull()?.firstChild

        val elemSibling = parameters.position.nextSibling
        val suffix = when {
            keyword == "acquires" && posParentNextSibling.elementType == L_BRACE -> {
                when {
                    context.nextCharIs(' ', 0) && context.nextCharIs(' ', 1) -> {
                        EditorModificationUtil.moveCaretRelatively(context.editor, 1)
                        ""
                    }
                    context.nextCharIs(' ') -> {
                        " "
                    }
                    else -> {
                        EditorModificationUtil.moveCaretRelatively(context.editor, -1)
                        "  "
                    }
                }
            }
            elemSibling != null && elemSibling.isWhitespace() -> ""
            posParentNextSibling == null || !context.alreadyHasSpace -> " "
            else -> ""
        }
        context.addSuffix(suffix)
    }
}
