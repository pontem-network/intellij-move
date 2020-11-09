package org.move.lang.core.completion

import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.completion.PrioritizedLookupElement
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.editor.EditorModificationUtil
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.compactText

const val DEFAULT_PRIORITY = 0.0

const val KEYWORD_PRIORITY = 80.0
const val PRIMITIVE_TYPE_PRIORITY = KEYWORD_PRIORITY

const val BUILTIN_FUNCTION_PRIORITY = 10.0
const val FUNCTION_PRIORITY = 10.0

//const val FRAGMENT_SPECIFIER_PRIORITY = KEYWORD_PRIORITY
const val VARIABLE_PRIORITY = 5.0

//const val ENUM_VARIANT_PRIORITY = 4.0
const val FIELD_DECL_PRIORITY = 3.0
//const val ASSOC_FN_PRIORITY = 2.0
//const val DEFAULT_PRIORITY = 0.0
//const val MACRO_PRIORITY = -0.1
//const val DEPRECATED_PRIORITY = -1.0

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
fun functionInsertHandler(isSpec: Boolean, hasParams: Boolean): InsertHandler<LookupElement> =
    InsertHandler<LookupElement> { ctx, _ ->
        if (isSpec) {
            if (!ctx.alreadyHasSpace) ctx.addSuffix(" ")
        } else {
            if (!ctx.alreadyHasCallParens) {
                ctx.document.insertString(ctx.selectionEndOffset, "()")
            }
            EditorModificationUtil.moveCaretRelatively(
                ctx.editor,
                if (hasParams) 1 else 2
            )
        }
    }

fun MoveNamedElement.createLookupElement(isSpecIdentifier: Boolean): LookupElement {
    val insertHandler = DefaultInsertHandler(isSpecIdentifier)
    return when (this) {
        is MoveModuleImport ->
            LookupElementBuilder
                .createWithIcon(this)
                .withLookupString(this.name ?: "")
                .withInsertHandler { context, _ ->
                    val document = context.document
                    if (!context.alreadyHasColonColon) {
                        document.insertString(context.selectionEndOffset, "::")
                        EditorModificationUtil.moveCaretRelatively(
                            context.editor,
                            2
                        )
                    }
                }

        is MoveFunctionSignatureOwner -> LookupElementBuilder.createWithIcon(this)
            .withLookupString(this.name ?: "")
            .withTailText(this.functionParameterList?.compactText ?: "()")
            .withTypeText(this.returnType?.type?.text ?: "()")
            .withInsertHandler(insertHandler)
//            .withInsertHandler { ctx, _ ->
//                if (isSpecIdentifier) {
//                    if (!ctx.alreadyHasSpace) ctx.addSuffix(" ")
//                } else {
//                    if (!ctx.alreadyHasCallParens) {
//                        ctx.document.insertString(ctx.selectionEndOffset, "()")
//                    }
//                    EditorModificationUtil.moveCaretRelatively(
//                        ctx.editor,
//                        if (this.parameters.isEmpty()) 2 else 1
//                    )
//                }
//            }

        is MoveConstDef -> LookupElementBuilder.createWithIcon(this)
            .withLookupString(this.name ?: "")
            .withTypeText(this.type?.text)

        is MoveStructDef -> LookupElementBuilder.createWithIcon(this)
            .withLookupString(this.name ?: "")
            .withTailText(" { ... }")
            .withInsertHandler(insertHandler)
//            .withInsertHandler { ctx, _ ->
//                if (isSpecIdentifier && !ctx.alreadyHasSpace)
//                    ctx.addSuffix(" ")
//            }

        is MoveStructFieldDef -> LookupElementBuilder
            .createWithIcon(this)
            .withLookupString(this.name ?: "")
            .withTypeText(this.type?.text)

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
fun InsertionContext.addSuffix(suffix: String) {
    document.insertString(selectionEndOffset, suffix)
    EditorModificationUtil.moveCaretRelatively(editor, suffix.length)
}

val InsertionContext.alreadyHasCallParens: Boolean
    get() = nextCharIs('(')

val InsertionContext.alreadyHasColonColon: Boolean
    get() = nextCharIs(':')

val InsertionContext.alreadyHasSpace: Boolean
    get() = nextCharIs(' ')

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

fun LookupElementBuilder.withPriority(priority: Double): LookupElement =
    if (priority == DEFAULT_PRIORITY) this else PrioritizedLookupElement.withPriority(this, priority)


class AngleBracketsInsertHandler : InsertHandler<LookupElement> {

    override fun handleInsert(context: InsertionContext, item: LookupElement) {
        val document = context.document
        if (!context.alreadyHasAngleBrackets) {
            document.insertString(context.selectionEndOffset, "<>")
        }
        EditorModificationUtil.moveCaretRelatively(context.editor, 1)
    }
}


class DefaultInsertHandler(private val isSpecIdentifier: Boolean) : InsertHandler<LookupElement> {
    override fun handleInsert(context: InsertionContext, item: LookupElement) {
        val document = context.document
        val element = item.psiElement as? MoveElement ?: return

//        if (element is MoveTypeParametersOwner) {
//            addGenericTypeCompletion(element, document, context)
//        }

        when (element) {
            is MoveFunctionSignatureOwner -> {
//                val angleBrackets = element.hasTypeParameters && !isSpecIdentifier
//                if (angleBrackets) {
//                    if (!context.alreadyHasAngleBrackets) {
//                        document.insertString(context.selectionEndOffset, "<>")
//                    }
//                    EditorModificationUtil.moveCaretRelatively(context.editor, 1)
//                }
                if (isSpecIdentifier) {
                    if (!context.alreadyHasSpace) context.addSuffix(" ")
                } else {
                    if (!context.alreadyHasCallParens) {
                        document.insertString(context.selectionEndOffset, "()")
                    }
//                    if (!angleBrackets) {
                    EditorModificationUtil.moveCaretRelatively(
                        context.editor,
                        if (element.parameters.isEmpty()) 2 else 1
                    )
//                    }
                }
            }
            is MoveStructDef -> {
                if (isSpecIdentifier && !context.alreadyHasSpace)
                    context.addSuffix(" ")

                if (element.hasTypeParameters && !isSpecIdentifier) {
                    if (!context.alreadyHasAngleBrackets) {
                        document.insertString(context.selectionEndOffset, "<>")
                    }
                    EditorModificationUtil.moveCaretRelatively(context.editor, 1)
                }
            }
        }
    }

}

//private fun addGenericTypeCompletion(
//    element: MoveTypeParametersOwner,
//    document: Document,
//    context: InsertionContext,
//) {
//    // complete only types that have at least one type parameter without a default
//    if (element.typeParameters.isEmpty()) return
//
//    if (!context.alreadyHasAngleBrackets) {
//        document.insertString(context.selectionEndOffset, "<>")
//    }
//
//    if (element is MoveFunctionSignatureOwner) {
//        // functions
//        if (!context.alreadyHasCallParens) {
//            document.insertString(context.selectionEndOffset, "()")
//        }
//    } else {
//        // structs
//        if (!context.alreadyHasAngleBrackets) {
//            document.insertString(context.selectionEndOffset, "<>")
//        }
//    }


// complete angle brackets only in a type context
//    val path = context.getElementOfType<RsPath>()
//    if (path == null || path.parent !is RsTypeReference) return

//    if (element.isFnLikeTrait) {
//        if (!context.alreadyHasCallParens) {
//            document.insertString(context.selectionEndOffset, "()")
//            context.doNotAddOpenParenCompletionChar()
//        }
//    } else {
//    }
//
//    EditorModificationUtil.moveCaretRelatively(context.editor, 1)
//}
