package org.move.lang.core.completion

import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.completion.PrioritizedLookupElement
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.editor.EditorModificationUtil
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiUtilBase
import com.intellij.psi.util.PsiUtilCore
import org.move.ide.presentation.shortPresentableText
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.ancestorStrict
import org.move.lang.core.psi.ext.inferBindingPatTy
import org.move.lang.core.psi.ext.owner
import org.move.lang.core.psi.ext.parametersText

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

//open class MvDefaultInsertHandler : InsertHandler<LookupElement> {
//    final override fun handleInsert(context: InsertionContext, item: LookupElement) {
//        val element = item.psiElement as? MvElement ?: return
//        handleInsert(element, context, item)
//    }
//
//    protected open fun handleInsert(
//        element: MvElement,
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

fun MvNamedElement.createLookupElement(isSpecIdentifier: Boolean): LookupElement {
    val insertHandler = DefaultInsertHandler(isSpecIdentifier)
    return when (this) {
        is MvModuleImport ->
            LookupElementBuilder
                .createWithIcon(this)
                .withLookupString(this.name ?: "")
//                .withInsertHandler { context, _ ->
//                    val document = context.document
//                    if (!context.alreadyHasColonColon) {
//                        document.insertString(context.selectionEndOffset, "::")
//                        EditorModificationUtil.moveCaretRelatively(
//                            context.editor,
//                            2
//                        )
//                        AutoPopupController.getInstance(context.project).scheduleAutoPopup(context.editor)
//                    }
//                }

        is MvFunctionSignatureOwner -> LookupElementBuilder.createWithIcon(this)
            .withLookupString(this.name ?: "")
            .withTailText(this.functionParameterList?.parametersText ?: "()")
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

//        is MvConstDef -> LookupElementBuilder.createWithIcon(this)
//            .withLookupString(this.name ?: "")
//            .withTypeText(this.typeAnnotation?.type?.text)

        is MvModuleDef -> LookupElementBuilder.createWithIcon(this)
            .withLookupString(this.name ?: "")
            .withTypeText(this.containingFile?.name)

        is MvStructSignature -> LookupElementBuilder.createWithIcon(this)
            .withLookupString(this.name ?: "")
            .withTailText(" { ... }")
            .withInsertHandler(insertHandler)
//            .withInsertHandler { ctx, _ ->
//                if (isSpecIdentifier && !ctx.alreadyHasSpace)
//                    ctx.addSuffix(" ")
//            }

        is MvStructFieldDef -> LookupElementBuilder
            .createWithIcon(this)
            .withLookupString(this.name ?: "")
            .withTypeText(this.typeAnnotation?.type?.text)

//        is MvFunctionParameter -> LookupElementBuilder.createWithIcon(this)
//            .withLookupString(this.name ?: "")
//            .withTypeText(this.typeAnnotation?.type?.text)

        is MvBindingPat -> LookupElementBuilder.createWithIcon(this)
            .withLookupString(this.name ?: "")
            .withTypeText(this.inferBindingPatTy().shortPresentableText(true))

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
        val element = item.psiElement as? MvElement ?: return

//        if (element is MvTypeParametersOwner) {
//            addGenericTypeCompletion(element, document, context)
//        }

        when (element) {
            is MvFunctionSignatureOwner -> {
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
            is MvStructSignature -> {
                if (isSpecIdentifier && !context.alreadyHasSpace)
                    context.addSuffix(" ")

                val insideAcquiresType =
                    context.file
                        .findElementAt(context.startOffset)
                        ?.ancestorStrict<MvAcquiresType>() != null
                if (element.hasTypeParameters && !isSpecIdentifier && !insideAcquiresType) {
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
//    element: MvTypeParametersOwner,
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
//    if (element is MvFunctionSignatureOwner) {
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
