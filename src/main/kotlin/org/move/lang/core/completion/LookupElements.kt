package org.move.lang.core.completion

import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.editor.EditorModificationUtil
import org.move.lang.core.psi.MoveFunctionDef
import org.move.lang.core.psi.MoveNamedElement
import org.move.lang.core.psi.ext.params

fun MoveNamedElement.createLookupElement(): LookupElement {
    return when (this) {
        is MoveFunctionDef -> LookupElementBuilder.create(this)
            .withLookupString(name ?: "")
            .withTailText(this.functionParameterList?.text ?: "()")
            .withInsertHandler { context: InsertionContext, _: LookupElement ->
                if (!context.alreadyHasCallParens) {
                    context.document.insertString(context.selectionEndOffset, "()")
                }
                EditorModificationUtil.moveCaretRelatively(
                    context.editor,
                    if (this.params.isEmpty()) 2 else 1
                )
            }

        else -> LookupElementBuilder.create(this).withLookupString(name ?: "")
    }
}

val InsertionContext.alreadyHasCallParens: Boolean
    get() = nextCharIs('(')

private val InsertionContext.alreadyHasAngleBrackets: Boolean
    get() = nextCharIs('<')

fun InsertionContext.nextCharIs(c: Char): Boolean =
    document.charsSequence.indexOfSkippingSpace(c, tailOffset) != null

private fun CharSequence.indexOfSkippingSpace(c: Char, startIndex: Int): Int? {
    for (i in startIndex until this.length) {
        val currentChar = this[i]
        if (c == currentChar) return i
        if (currentChar != ' ' && currentChar != '\t') return null
    }
    return null
}

