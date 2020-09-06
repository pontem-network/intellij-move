package org.move.lang.core.completion

import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.editor.EditorModificationUtil
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.compactText
import org.move.lang.core.psi.ext.params

//open class MoveDefaultInsertHandler : InsertHandler<LookupElement> {
//    final override fun handleInsert(context: InsertionContext, item: LookupElement) {
//        val element = item.psiElement as? MoveElement ?: return
//        handleInsert(element, context, item)
//    }
//
//    protected open fun handleInsert(
//        element: MoveElement,
//        context: InsertionContext,
//        item: LookupElement
//    ) {
//        val document = context.document
//        val startOffset = context.startOffset
//
//    }
//}

fun MoveNamedElement.createLookupElement(): LookupElement {
    return when (this) {
        is MoveFunctionDef -> LookupElementBuilder.createWithIcon(this)
            .withLookupString(this.name ?: "")
            .withTailText(this.functionParameterList?.compactText ?: "()")
            .withTypeText(this.returnType?.type?.text ?: "()")
            .withInsertHandler { context: InsertionContext, _: LookupElement ->
                if (!context.alreadyHasCallParens) {
                    context.document.insertString(context.selectionEndOffset, "()")
                }
                EditorModificationUtil.moveCaretRelatively(
                    context.editor,
                    if (this.params.isEmpty()) 2 else 1
                )
            }

        is MoveConstDef -> LookupElementBuilder.createWithIcon(this)
            .withLookupString(this.name ?: "")
            .withTypeText(this.type?.text)

        is MoveStructDef -> LookupElementBuilder.createWithIcon(this)
            .withLookupString(this.name ?: "")
            .withTailText(" { ... }")

        is MoveFunctionParameter -> LookupElementBuilder.createWithIcon(this)
            .withLookupString(this.name ?: "")
            .withTypeText(this.type?.text)

        is MoveBindingPat -> LookupElementBuilder.createWithIcon(this)
            .withLookupString(this.name ?: "")

        else -> LookupElementBuilder.create(this).withLookupString(name ?: "")
    }
}

//fun createLookupElement(
//    scopeEntry: ScopeEntry,
//    context: RsCompletionContext,
//    locationString: String? = null,
//    insertHandler: InsertHandler<LookupElement> = RsDefaultInsertHandler()
//): LookupElement {
//    val completionEntity = ScopedBaseCompletionEntity(scopeEntry)
//    return createLookupElement(completionEntity, context, locationString, insertHandler)
//}
//
//fun createLookupElement(
//    completionEntity: CompletionEntity,
//    context: RsCompletionContext,
//    locationString: String? = null,
//    insertHandler: InsertHandler<LookupElement> = RsDefaultInsertHandler()
//): LookupElement {
//    val lookup = completionEntity.createBaseLookupElement(context)
//        .withInsertHandler(insertHandler)
//        .let { if (locationString != null) it.appendTailText(" ($locationString)", true) else it }
//    var priority = completionEntity.getBasePriority(context)
//
//    if (isCompatibleTypes(completionEntity.implLookup, completionEntity.ty, context.expectedTy)) {
//        priority += EXPECTED_TYPE_PRIORITY_OFFSET
//    }
//
//    return lookup.withPriority(priority)
//}

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

