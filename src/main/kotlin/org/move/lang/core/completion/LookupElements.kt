package org.move.lang.core.completion

import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.completion.PrioritizedLookupElement
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.editor.EditorModificationUtil
import org.move.cli.AddressVal
import org.move.ide.MoveIcons
import org.move.ide.presentation.shortPresentableText
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.*
import org.move.lang.core.resolve.ref.Namespace
import org.move.lang.core.types.infer.inferenceCtx

const val KEYWORD_PRIORITY = 80.0

const val ITEM_WITH_EXPECTED_TYPE_PRIORITY = 40.0

const val LOCAL_ITEM_PRIORITY = 40.0
const val BUILTIN_ITEM_PRIORITY = 30.0

const val IMPORTED_ITEM_PRIORITY = 15.0
const val IMPORTED_MODULE_PRIORITY = 15.0

const val UNIMPORTED_ITEM_PRIORITY = 5.0
const val UNIMPORTED_MODULE_PRIORITY = 5.0

const val DEFAULT_PRIORITY = 0.0

const val PRIMITIVE_TYPE_PRIORITY = KEYWORD_PRIORITY

const val MACRO_PRIORITY = 30.0
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

fun MvNamedElement.createLookupElement(): LookupElementBuilder {
    return LookupElementBuilder
        .createWithIcon(this)
        .withLookupString(this.name ?: "")
}

fun MvModule.createSelfLookup(): LookupElement {
    return LookupElementBuilder
        .create("Self")
        .withBoldness(true)
}

fun AddressVal.createCompletionLookupElement(lookupString: String): LookupElement {
    return LookupElementBuilder
        .create(lookupString)
        .withIcon(MoveIcons.ADDRESS)
        .withTypeText(packageName)
}

fun MvNamedElement.createCompletionLookupElement(
    insertHandler: InsertHandler<LookupElement> = MvInsertHandler(),
    ns: Set<Namespace> = emptySet(),
    priority: Double = DEFAULT_PRIORITY,
): LookupElement {
    var lookupElement = when (this) {
        is MvModuleUseSpeck -> {
            val module = this.fqModuleRef?.reference?.resolve()
            if (module != null) {
                return module.createCompletionLookupElement(insertHandler, ns)
            } else {
                this.createLookupElement()
            }
        }

        is MvUseItem -> {
            val namedItem = this.reference.resolve()
            if (namedItem != null) {
                return namedItem.createCompletionLookupElement(insertHandler, ns)
            } else {
                this.createLookupElement()
            }
        }

        is MvFunction -> this.createLookupElement()
            .withTailText(this.signatureText)
            .withTypeText(this.outerFileName)

        is MvSpecFunction -> this.createLookupElement()
            .withTailText(this.functionParameterList?.parametersText ?: "()")
            .withTypeText(this.returnType?.type?.text ?: "()")

        is MvModule -> this.createLookupElement()
            .withTailText(this.address()?.let { " ${it.text}" } ?: "")
            .withTypeText(this.containingFile?.name)

        is MvStruct -> {
            val tailText = if (Namespace.TYPE !in ns) " { ... }" else ""
            this.createLookupElement()
                .withTailText(tailText)
                .withTypeText(this.containingFile?.name)
        }

        is MvStructField -> this.createLookupElement()
            .withTypeText(this.typeAnnotation?.type?.text)

        is MvBindingPat -> this.createLookupElement()
            .withTypeText(this.cachedTy(this.inferenceCtx(this.isMsl())).shortPresentableText(true))

        is MvSchema -> this.createLookupElement()
            .withTypeText(this.containingFile?.name)

        else -> LookupElementBuilder.create(this)
    }
    lookupElement = lookupElement.withInsertHandler(insertHandler)
    return PrioritizedLookupElement.withPriority(lookupElement, priority)
}

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

private fun InsertionContext.functionSuffixAndOffset(
    requiredTypeParams: List<MvTypeParameter>,
    parameters: List<MvFunctionParameter>
): Pair<String, Int> {
    var suffix = ""
    if (!this.alreadyHasAngleBrackets && requiredTypeParams.isNotEmpty()) {
        suffix += "<>"
    }
    if (!this.alreadyHasAngleBrackets && !this.alreadyHasCallParens) {
        suffix += "()"
    }
    val offset = when {
        parameters.isNotEmpty() || requiredTypeParams.isNotEmpty() -> 1
        else -> 2
    }
    return Pair(suffix, offset)
}

open class MvInsertHandler : InsertHandler<LookupElement> {
    override fun handleInsert(context: InsertionContext, item: LookupElement) {
        val document = context.document
        val element = item.psiElement as? MvElement ?: return

        when (element) {
            is MvFunctionLike -> {
                val requiredTypeParams = element.typeParamsUsedOnlyInReturnType
                val (suffix, offset) = context.functionSuffixAndOffset(requiredTypeParams, element.parameters)

                document.insertString(context.selectionEndOffset, suffix)
                EditorModificationUtil.moveCaretRelatively(context.editor, offset)
            }
            is MvSchema -> {
                if (element.hasTypeParameters) {
                    if (!context.alreadyHasAngleBrackets) {
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
                    if (!context.alreadyHasAngleBrackets) {
                        document.insertString(context.selectionEndOffset, "<>")
                    }
                    EditorModificationUtil.moveCaretRelatively(context.editor, 1)
                }
            }
        }
    }
}
