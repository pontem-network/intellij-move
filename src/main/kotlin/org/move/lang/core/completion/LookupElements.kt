package org.move.lang.core.completion

import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.completion.PrioritizedLookupElement
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.editor.EditorModificationUtil
import org.move.ide.presentation.text
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.*
import org.move.lang.core.resolve.ItemVis
import org.move.lang.core.resolve.ref.Namespace
import org.move.lang.core.types.infer.InferenceContext
import org.move.lang.core.types.infer.containsTyOfClass
import org.move.lang.core.types.infer.instantiateItemTy
import org.move.lang.core.types.infer.ownerInferenceCtx
import org.move.lang.core.types.ty.Ty
import org.move.lang.core.types.ty.TyFunction
import org.move.lang.core.types.ty.TyInfer

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
            .withTailText(this.address()?.let { " ${it.text}" } ?: "")
            .withTypeText(this.containingFile?.name)

        is MvStruct -> {
            val tailText = if (Namespace.TYPE !in ns) " { ... }" else ""
            this.createLookupElementWithIcon()
                .withTailText(tailText)
                .withTypeText(this.containingFile?.name)
        }

        is MvStructField -> this.createLookupElementWithIcon()
            .withTypeText(this.typeAnnotation?.type?.text)

        is MvBindingPat -> this.createLookupElementWithIcon()
            .withTypeText(this.inferredTy(this.ownerInferenceCtx(this.isMsl())).text(true))

        is MvSchema -> this.createLookupElementWithIcon()
            .withTypeText(this.containingFile?.name)

        else -> LookupElementBuilder.create(this)
            .withLookupString(this.name ?: "")
    }
}

data class CompletionContext(
    val contextElement: MvElement,
    val itemVis: ItemVis,
    val expectedTy: Ty? = null,
)


fun MvNamedElement.createLookupElement(
    context: CompletionContext,
    priority: Double = DEFAULT_PRIORITY,
    insertHandler: InsertHandler<LookupElement> = DefaultInsertHandler(context),
): LookupElement {
    val lookupElement = this.createBaseLookupElement(context.itemVis.namespaces)
    val props = lookupProperties(this, context)
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
            is MvFunctionLike -> {
                val allTypesInferred = run {
                    // explicit type arguments required
                    if (element.requiredTypeParams.isNotEmpty()) return@run false
                    // all type arguments inferrable from function parameters
                    if (element.typeParamsUsedOnlyInReturnType.isEmpty()) return@run true

                    if (completionContext == null) return@run false
                    val msl = element.isMsl()
                    val inferenceCtx = InferenceContext(msl)
                    val funcTy = instantiateItemTy(element, inferenceCtx) as? TyFunction ?: return@run false

                    val expectedTy = completionContext.expectedTy
                    if (expectedTy != null) {
                        inferenceCtx.addConstraint(funcTy.retType, expectedTy)
                    }
                    inferenceCtx.processConstraints()

                    val resolvedTy = inferenceCtx.resolveTy(funcTy)
                    !resolvedTy.containsTyOfClass(listOf(TyInfer::class.java))
                }

                var suffix = ""
                if (!context.hasAngleBrackets && !allTypesInferred) {
                    suffix += "<>"
                }
                if (!context.hasAngleBrackets && !context.hasCallParens) {
                    suffix += "()"
                }
                val offset = when {
                    element.parameters.isNotEmpty() || !allTypesInferred -> 1
                    else -> 2
                }

                document.insertString(context.selectionEndOffset, suffix)
                EditorModificationUtil.moveCaretRelatively(context.editor, offset)
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
