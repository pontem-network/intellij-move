package org.move.lang.core.completion

import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.completion.PrioritizedLookupElement
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.editor.EditorModificationUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.MvMethodOrField
import org.move.lang.core.psi.ext.ancestorOrSelf
import org.move.lang.core.resolve.ScopeEntry
import org.move.lang.core.resolve.ref.ResolutionContext
import org.move.lang.core.types.infer.Substitution
import org.move.lang.core.types.infer.emptySubstitution
import org.move.lang.core.types.ty.Ty

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

data class MvCompletionContext(
    val contextElement: MvElement,
    val msl: Boolean,
    val expectedTy: Ty? = null,
    val resolutionCtx: ResolutionContext? = null,
    val structAsType: Boolean = false
)

data class Completions(
    val ctx: MvCompletionContext,
    val result: CompletionResultSet
) {
    fun addCompletionItem(completionItem: CompletionItem) {
        result.addElement(completionItem)
    }

    fun addEntry(
        entry: ScopeEntry,
        applySubst: Substitution = emptySubstitution,
    ) {
        result.addElement(
            createCompletionItem(
                entry,
                ctx,
                subst = applySubst,
                priority = entry.element.completionPriority
            )
        )
    }

    fun addEntries(entries: List<ScopeEntry>, applySubst: Substitution = emptySubstitution) {
        val completionItems =
            entries.map {
                createCompletionItem(
                    scopeEntry = it,
                    completionContext = ctx,
                    priority = it.element.completionPriority,
                    subst = applySubst,
                )
            };
        result.addAllElements(completionItems)
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

val InsertionContext.alreadyHasAngleBrackets: Boolean
    get() = nextCharIs('<')

fun InsertionContext.nextCharIs(c: Char): Boolean =
    nextCharIs(c, 0)

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
        if (!context.alreadyHasAngleBrackets) {
            document.insertString(context.selectionEndOffset, "<>")
        }
        EditorModificationUtil.moveCaretRelatively(context.editor, 1)
    }
}

open class DefaultInsertHandler(val completionCtx: MvCompletionContext? = null): InsertHandler<LookupElement> {

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

        val itemSpecRef = context.getElementOfType<MvItemSpecRef>()
        if (itemSpecRef != null) {
            // inserting item in `spec /*caret*/ {}`, no need for the signature, just insert with leading space
            if (!context.alreadyHasSpace) {
                context.addSuffix(" ")
            }
            return
        }

        when (element) {
            is MvFunctionLike -> {
                // no suffix for imports
                if (completionCtx?.resolutionCtx?.isUseSpeck == true) return

                val isMethodCall = context.getElementOfType<MvMethodOrField>() != null
                val requiresExplicitTypes =
                    element.requiresExplicitlyProvidedTypeArguments(completionCtx)
                if (isMethodCall) {
                    var suffix = ""
                    if (requiresExplicitTypes && !context.alreadyHasColonColon) {
                        suffix += "::<>"
                    }
                    if (!context.alreadyHasColonColon && !context.alreadyHasCallParens) {
                        suffix += "()"
                    }
                    val caretShift = when {
                        context.alreadyHasColonColon || requiresExplicitTypes -> 3
                        // drop first for self
                        element.parameters.drop(1).isNotEmpty() -> 1
                        else -> 2
                    }
                    context.document.insertString(context.selectionEndOffset, suffix)
                    EditorModificationUtil.moveCaretRelatively(context.editor, caretShift)
                } else {
                    var suffix = ""
                    if (requiresExplicitTypes && !context.alreadyHasAngleBrackets) {
                        suffix += "<>"
                    }
                    if (!context.alreadyHasAngleBrackets && !context.alreadyHasCallParens) {
                        suffix += "()"
                    }
                    val caretShift = when {
                        requiresExplicitTypes -> 1
                        element.parameters.isNotEmpty() -> 1
                        else -> 2
                    }
                    context.document.insertString(context.selectionEndOffset, suffix)
                    EditorModificationUtil.moveCaretRelatively(context.editor, caretShift)
                }
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

// When a user types `(` while completion,
// `com.intellij.codeInsight.completion.DefaultCharFilter` invokes completion with selected item.
// And if we insert `()` for the item (for example, function), a user get double parentheses
private fun InsertionContext.doNotAddOpenParenCompletionChar() {
    if (completionChar == '(') {
        setAddCompletionChar(false)
    }
}

inline fun <reified T: PsiElement> InsertionContext.getElementOfType(strict: Boolean = false): T? =
    PsiTreeUtil.findElementOfClassAtOffset(file, tailOffset - 1, T::class.java, strict)
