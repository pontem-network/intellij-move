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
import org.move.lang.core.resolve.ref.Namespace
import org.move.lang.core.types.infer.*
import org.move.lang.core.types.ty.Ty
import org.move.lang.core.types.ty.TyFunction
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

fun MvNamedElement.createBaseLookupElement(ns: Set<Namespace>): LookupElementBuilder {
    return when (this) {
        is MvModuleUseSpeck -> {
            val module = this.fqModuleRef?.reference?.resolve()
            if (module != null) {
                module.createBaseLookupElement(ns)
            } else {
                this.createLookupElementWithIcon()
            }
        }

        is MvUseItem -> {
            val namedItem = this.reference.resolve()
            if (namedItem != null) {
                namedItem.createBaseLookupElement(ns)
            } else {
                this.createLookupElementWithIcon()
            }
        }

        is MvFunction -> this.createLookupElementWithIcon()
            .withTailText(this.signatureText)
            .withTypeText(this.outerFileName)

        is MvSpecFunction -> this.createLookupElementWithIcon()
            .withTailText(this.functionParameterList?.parametersText ?: "()")
            .withTypeText(this.returnType?.type?.text ?: "()")

        is MvModule -> this.createLookupElementWithIcon()
            .withTailText(this.addressRef()?.let { " ${it.text}" } ?: "")
            .withTypeText(this.containingFile?.name)

        is MvStruct -> {
            val tailText = if (Namespace.TYPE !in ns) " { ... }" else ""
            this.createLookupElementWithIcon()
                .withTailText(tailText)
                .withTypeText(this.containingFile?.name)
        }

        is MvStructField -> this.createLookupElementWithIcon()
            .withTypeText(this.typeAnnotation?.type?.text)

        is MvConst -> {
            val msl = this.isMsl()
            val constTy = this.type?.loweredType(msl) ?: TyUnknown
            this.createLookupElementWithIcon()
                .withTypeText(constTy.text(true))
        }

        is MvBindingPat -> {
            val msl = this.isMsl()
            val inference = this.inference(msl)
            // race condition sometimes happens, when file is too big, inference is not finished yet
            val ty = inference?.getPatTypeOrUnknown(this) ?: TyUnknown
            this.createLookupElementWithIcon()
                .withTypeText(ty.text(true))
        }

        is MvSchema -> this.createLookupElementWithIcon()
            .withTypeText(this.containingFile?.name)

        else -> LookupElementBuilder.create(this)
            .withLookupString(this.name ?: "")
    }
}

data class CompletionContext(
    val contextElement: MvElement,
    val namespaces: Set<Namespace>,
    val contextScopeInfo: ContextScopeInfo,
    val expectedTy: Ty? = null,
)


fun MvNamedElement.createLookupElement(
    completionContext: CompletionContext,
    priority: Double = DEFAULT_PRIORITY,
    insertHandler: InsertHandler<LookupElement> = DefaultInsertHandler(completionContext),
): LookupElement {
    val lookupElement = this.createBaseLookupElement(completionContext.namespaces)
    val props = lookupProperties(this, completionContext)
    return lookupElement
        .withInsertHandler(insertHandler)
        .withPriority(priority)
        .toMvLookupElement(props)
}

fun MvNamedElement.createCompletionLookupElement(
    insertHandler: InsertHandler<LookupElement> = DefaultInsertHandler(),
    ns: Set<Namespace> = emptySet(),
    priority: Double = DEFAULT_PRIORITY,
    props: LookupElementProperties = LookupElementProperties()
): LookupElement {
    val lookupElement = this.createBaseLookupElement(ns)
    return lookupElement
        .withInsertHandler(insertHandler)
        .withPriority(priority)
        .toMvLookupElement(props)
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

private val InsertionContext.hasAngleBrackets: Boolean
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

class AngleBracketsInsertHandler : InsertHandler<LookupElement> {

    override fun handleInsert(context: InsertionContext, item: LookupElement) {
        val document = context.document
        if (!context.hasAngleBrackets) {
            document.insertString(context.selectionEndOffset, "<>")
        }
        EditorModificationUtil.moveCaretRelatively(context.editor, 1)
    }
}

open class DefaultInsertHandler(val completionContext: CompletionContext? = null) : InsertHandler<LookupElement> {
    override fun handleInsert(context: InsertionContext, item: LookupElement) {
        val document = context.document
        val element = item.psiElement as? MvElement ?: return

        when (element) {
            is MvFunctionLike -> handleFunctionInsert(context, element)
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

    private fun handleFunctionInsert(context: InsertionContext, element: MvFunctionLike) {
        val requiresExplicitTypes = run {
            val msl = element.isMsl()
            val callTy = element.declaredType(msl).substitute(element.tyInfers) as TyFunction

            val inferenceCtx = InferenceContext(msl)
            callTy.paramTypes.forEach {
                val resolvedParamType = it.foldTyInferWith { TyUnknown }
                inferenceCtx.combineTypes(it, resolvedParamType)
            }
            val expectedTy = completionContext?.expectedTy
            if (expectedTy != null && expectedTy !is TyUnknown) {
                inferenceCtx.combineTypes(callTy.retType, expectedTy)
            }
            (inferenceCtx.resolveTypeVarsIfPossible(callTy) as TyFunction).needsTypeAnnotation()
        }

        var suffix = ""
        if (!context.hasAngleBrackets && requiresExplicitTypes) {
            suffix += "<>"
        }
        if (!context.hasAngleBrackets && !context.hasCallParens) {
            suffix += "()"
        }

        val offset = when {
            element.parameters.isNotEmpty() || requiresExplicitTypes -> 1
            else -> 2
        }

        context.document.insertString(context.selectionEndOffset, suffix)
        EditorModificationUtil.moveCaretRelatively(context.editor, offset)
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

inline fun <reified T : PsiElement> InsertionContext.getElementOfType(strict: Boolean = false): T? =
    PsiTreeUtil.findElementOfClassAtOffset(file, tailOffset - 1, T::class.java, strict)
