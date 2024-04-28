package org.move.lang.core.completion

import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.completion.PrioritizedLookupElement
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.editor.EditorModificationUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.move.ide.presentation.text
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.*
import org.move.lang.core.resolve.ContextScopeInfo
import org.move.lang.core.types.infer.*
import org.move.lang.core.types.ty.Ty
import org.move.lang.core.types.ty.TyUnknown

const val KEYWORD_PRIORITY = 80.0

//const val ITEM_WITH_EXPECTED_TYPE_PRIORITY = 40.0

const val LOCAL_ITEM_PRIORITY = 40.0
const val BUILTIN_ITEM_PRIORITY = 30.0

//const val IMPORTED_ITEM_PRIORITY = 15.0
const val IMPORTED_MODULE_PRIORITY = 15.0

const val UNIMPORTED_ITEM_PRIORITY = 5.0
//const val UNIMPORTED_MODULE_PRIORITY = 5.0

const val DEFAULT_PRIORITY = 0.0

const val PRIMITIVE_TYPE_PRIORITY = KEYWORD_PRIORITY

const val MACRO_PRIORITY = 30.0
const val VECTOR_LITERAL_PRIORITY = 30.0
//const val BUILTIN_FUNCTION_PRIORITY = 10.0
//const val FUNCTION_PRIORITY = 10.0

//const val FRAGMENT_SPECIFIER_PRIORITY = KEYWORD_PRIORITY
//const val VARIABLE_PRIORITY = 5.0

//const val ENUM_VARIANT_PRIORITY = 4.0
//const val FIELD_DECL_PRIORITY = 3.0
//const val ASSOC_FN_PRIORITY = 2.0
//const val DEFAULT_PRIORITY = 0.0
//const val MACRO_PRIORITY = -0.1
//const val DEPRECATED_PRIORITY = -1.0

fun MvNamedElement.createLookupElementWithIcon(): LookupElementBuilder {
    return LookupElementBuilder
        .createWithIcon(this)
        .withLookupString(this.name ?: "")
}

@Suppress("UnusedReceiverParameter")
fun MvModule.createSelfLookup(): LookupElement {
    return LookupElementBuilder
        .create("Self")
        .withBoldness(true)
}

fun MvNamedElement.getLookupElementBuilder(
    completionCtx: CompletionContext,
    subst: Substitution = emptySubstitution,
    structAsType: Boolean = false
): LookupElementBuilder {
    val lookupElementBuilder = this.createLookupElementWithIcon()
    val msl = completionCtx.isMsl()
    return when (this) {
        is MvFunction -> {
            val signature = FuncSignature.fromFunction(this, msl).substitute(subst)
            if (completionCtx.contextElement is MvMethodOrField) {
                lookupElementBuilder
                    .withTailText(signature.paramsText())
                    .withTypeText(signature.retTypeText())
            } else {
                lookupElementBuilder
                    .withTailText("${signature.paramsText()}${signature.retTypeSuffix()}")
                    .withTypeText(this.outerFileName)
            }
        }
        is MvSpecFunction -> lookupElementBuilder
            .withTailText(this.parameters.joinToSignature())
            .withTypeText(this.returnType?.type?.text ?: "()")

        is MvModule -> lookupElementBuilder
            .withTailText(this.addressRef()?.let { " ${it.text}" } ?: "")
            .withTypeText(this.containingFile?.name)

        is MvStruct -> {
            val tailText = if (structAsType) "" else " { ... }"
            lookupElementBuilder
                .withTailText(tailText)
                .withTypeText(this.containingFile?.name)
        }

        is MvStructField -> {
            val fieldTy = this.type?.loweredType(msl)?.substitute(subst) ?: TyUnknown
            lookupElementBuilder
                .withTypeText(fieldTy.text(false))
        }
        is MvConst -> {
//            val msl = this.isMslOnlyItem
            val constTy = this.type?.loweredType(msl) ?: TyUnknown
            lookupElementBuilder
                .withTypeText(constTy.text(true))
        }

        is MvBindingPat -> {
//            val msl = this.isMslOnlyItem
            val bindingInference = this.inference(msl)
            // race condition sometimes happens, when file is too big, inference is not finished yet
            val ty = bindingInference?.getPatTypeOrUnknown(this) ?: TyUnknown
            lookupElementBuilder
                .withTypeText(ty.text(true))
        }

        is MvSchema -> lookupElementBuilder
            .withTypeText(this.containingFile?.name)

        else -> lookupElementBuilder
    }
}

data class CompletionContext(
    val contextElement: MvElement,
    val contextScopeInfo: ContextScopeInfo,
    val expectedTy: Ty? = null,
) {
    fun isMsl(): Boolean = contextScopeInfo.isMslScope
}


fun MvNamedElement.createLookupElement(
    completionContext: CompletionContext,
    subst: Substitution = emptySubstitution,
    structAsType: Boolean = false,
    priority: Double = DEFAULT_PRIORITY,
    insertHandler: InsertHandler<LookupElement> = DefaultInsertHandler(completionContext),
): LookupElement {
    val builder =
        this.getLookupElementBuilder(
            completionContext,
            subst = subst,
            structAsType = structAsType
        )
            .withInsertHandler(insertHandler)
            .withPriority(priority)
    val props = getLookupElementProperties(this, subst, completionContext)
    return builder.toMvLookupElement(properties = props)
}

fun InsertionContext.addSuffix(suffix: String) {
    document.insertString(selectionEndOffset, suffix)
    EditorModificationUtil.moveCaretRelatively(editor, suffix.length)
}

val InsertionContext.hasCallParens: Boolean
    get() = nextCharIs('(')

val InsertionContext.alreadyHasColonColon: Boolean
    get() = nextCharIs(':')

val InsertionContext.alreadyHasSpace: Boolean
    get() = nextCharIs(' ')

val InsertionContext.hasAngleBrackets: Boolean
    get() = nextCharIs('<')

fun InsertionContext.nextCharIs(c: Char): Boolean =
    nextCharIs(c, 0)
//    document.charsSequence.indexOfSkippingSpace(c, tailOffset) != null

fun InsertionContext.nextCharIs(c: Char, offset: Int): Boolean =
    document.charsSequence.indexOfSkippingSpace(c, tailOffset + offset) != null

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

class AngleBracketsInsertHandler: InsertHandler<LookupElement> {

    override fun handleInsert(context: InsertionContext, item: LookupElement) {
        val document = context.document
        if (!context.hasAngleBrackets) {
            document.insertString(context.selectionEndOffset, "<>")
        }
        EditorModificationUtil.moveCaretRelatively(context.editor, 1)
    }
}

open class DefaultInsertHandler(val completionCtx: CompletionContext? = null): InsertHandler<LookupElement> {

    final override fun handleInsert(context: InsertionContext, item: LookupElement) {
        val element = item.psiElement as? MvElement ?: return
        handleInsert(element, context, item)
    }

    protected open fun handleInsert(
        element: MvElement,
        context: InsertionContext,
        item: LookupElement
    ) {
        val document = context.document
        when (element) {
            is MvFunctionLike -> {
                val requiresExplicitTypeArguments =
                    element.requiresExplicitlyProvidedTypeArguments(completionCtx)
                var suffix = ""
                if (!context.hasAngleBrackets && requiresExplicitTypeArguments) {
                    suffix += "<>"
                }
                if (!context.hasAngleBrackets && !context.hasCallParens) {
                    suffix += "()"
                }
                val isMethodCall = context.getElementOfType<MvMethodOrField>() != null
                val fnParameters = if (isMethodCall) element.parameters.drop(1) else element.parameters
                val caretShift = when {
                    requiresExplicitTypeArguments -> 1
                    fnParameters.isNotEmpty() -> 1
                    else -> 2
                }
                context.document.insertString(context.selectionEndOffset, suffix)
                EditorModificationUtil.moveCaretRelatively(context.editor, caretShift)
            }
            is MvSchema -> {
                if (element.hasTypeParameters) {
                    if (!context.hasAngleBrackets) {
                        document.insertString(context.selectionEndOffset, "<>")
                    }
                    EditorModificationUtil.moveCaretRelatively(context.editor, 1)
                }
            }
            is MvStruct -> {
                val insideAcquiresType =
                    context.file
                        .findElementAt(context.startOffset)
                        ?.ancestorOrSelf<MvAcquiresType>() != null
                if (element.hasTypeParameters && !insideAcquiresType) {
                    if (!context.hasAngleBrackets) {
                        document.insertString(context.selectionEndOffset, "<>")
                    }
                    EditorModificationUtil.moveCaretRelatively(context.editor, 1)
                }
            }
        }
    }
}

// When a user types `(` while completion,
// `com.intellij.codeInsight.completion.DefaultCharFilter` invokes completion with selected item.
// And if we insert `()` for the item (for example, function), a user get double parentheses
private fun InsertionContext.doNotAddOpenParenCompletionChar() {
    if (completionChar == '(') {
        setAddCompletionChar(false)
//        Testmarks.DoNotAddOpenParenCompletionChar.hit()
    }
}

inline fun <reified T: PsiElement> InsertionContext.getElementOfType(strict: Boolean = false): T? =
    PsiTreeUtil.findElementOfClassAtOffset(file, tailOffset - 1, T::class.java, strict)
