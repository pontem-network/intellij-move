package org.move.lang.core.completion

import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.completion.PrioritizedLookupElement
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.editor.EditorModificationUtil
import org.move.ide.presentation.shortPresentableText
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.*

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

fun MvNamedElement.createLookupElement(
    insertHandler: InsertHandler<LookupElement> = MvInsertHandler()
): LookupElement {
    return when (this) {
        is MvModuleImport ->
            LookupElementBuilder
                .createWithIcon(this)
                .withLookupString(this.name ?: "")

        is MvFunction -> LookupElementBuilder.createWithIcon(this)
            .withLookupString(this.name ?: "")
            .withTailText(this.functionParameterList?.parametersText ?: "()")
            .withTypeText(this.returnType?.type?.text ?: "()")
            .withInsertHandler(insertHandler)

        is MvModuleDef -> LookupElementBuilder.createWithIcon(this)
            .withLookupString(this.name ?: "")
            .withTypeText(this.containingFile?.name)

        is MvStruct_ -> LookupElementBuilder.createWithIcon(this)
            .withLookupString(this.name ?: "")
            .withTailText(" { ... }")
            .withInsertHandler(insertHandler)

        is MvStructFieldDef -> LookupElementBuilder
            .createWithIcon(this)
            .withLookupString(this.name ?: "")
            .withTypeText(this.typeAnnotation?.type?.text)

        is MvBindingPat -> LookupElementBuilder.createWithIcon(this)
            .withLookupString(this.name ?: "")
            .withTypeText(this.inferBindingPatTy().shortPresentableText(true))

        else -> LookupElementBuilder.create(this).withLookupString(name ?: "")
    }
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

class MvInsertHandler : InsertHandler<LookupElement> {
    override fun handleInsert(context: InsertionContext, item: LookupElement) {
        val document = context.document
        val element = item.psiElement as? MvElement ?: return

        when (element) {
            is MvFunction -> {
                val reqTypeParams = element.requiredTypeParams
                var suffix = ""
                if (!context.alreadyHasAngleBrackets && reqTypeParams.isNotEmpty()) {
                    suffix += "<>"
                }
                if (!context.alreadyHasCallParens) {
                    suffix += "()"
                }
                document.insertString(context.selectionEndOffset, suffix)
                val offset = when {
                    reqTypeParams.isNotEmpty() || element.parameters.isNotEmpty() -> 1
                    else -> 2
                }
                EditorModificationUtil.moveCaretRelatively(context.editor, offset)
            }
            is MvStruct_ -> {
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
