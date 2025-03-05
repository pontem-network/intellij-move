package org.move.lang.core.completion.providers

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.editor.EditorModificationUtil
import com.intellij.patterns.ElementPattern
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.intellij.util.ProcessingContext
import org.move.cli.TomlAddress
import org.move.ide.MoveIcons
import org.move.lang.core.MvPsiPattern
import org.move.lang.core.completion.alreadyHasColonColon
import org.move.lang.core.psi.MvModule
import org.move.lang.core.psi.MvNamedAddress
import org.move.lang.core.psi.MvUseStmt
import org.move.lang.core.psiElement
import org.move.lang.core.withParent
import org.move.lang.moveProject

object AddressInModuleDeclCompletionProvider: MvCompletionProvider() {
    override val elementPattern: ElementPattern<out PsiElement>
        get() = PlatformPatterns
            .psiElement()
            .withParent<MvNamedAddress>()
            .withSuperParent(3, psiElement<MvModule>())

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val moveProject = parameters.position.moveProject ?: return
        val addresses = moveProject.numericTomlAddresses()
        for ((name, value) in addresses.entries.sortedBy { it.key }) {
            val lookup = LookupElementBuilder
                .create(name)
                .withTypeText(value.value)
                .withInsertHandler { ctx, _ ->
                    val document = ctx.document
                    if (!ctx.alreadyHasColonColon) {
                        document.insertString(ctx.selectionEndOffset, "::")
                    }
                    EditorModificationUtil.moveCaretRelatively(ctx.editor, 2)
                }
            result.addElement(lookup)
        }
    }
}

object NamedAddressAtValueExprCompletionProvider: MvCompletionProvider() {
    override val elementPattern: ElementPattern<out PsiElement>
        get() = PlatformPatterns
            .psiElement().withParent<MvNamedAddress>()
            .andNot(
                PlatformPatterns.psiElement()
                    .withSuperParent(3, psiElement<MvModule>())
            )

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val moveProject = parameters.position.moveProject ?: return
        val declaredNamedAddresses = moveProject.namedAddresses().values
        for ((name, addressVal) in declaredNamedAddresses.entries.sortedBy { it.key }) {
            val lookup = addressVal.createCompletionLookupElement(name)
            result.addElement(lookup)
        }
    }
}

object NamedAddressInUseStmtCompletionProvider: MvCompletionProvider() {
    override val elementPattern: ElementPattern<out PsiElement>
        get() = MvPsiPattern.path()
            .and(
                PlatformPatterns.psiElement()
                    // use path::ident::
                    //          ^ should not exist
                    .andNot(
                        PlatformPatterns.psiElement().afterLeaf("::")
                    )
                    // use [ident::]
                    //           ^ path (1)
                    //     ^ use speck (2)
                    // ^ use stmt (3)
                    .withSuperParent(3, psiElement<MvUseStmt>())
            )

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val element = parameters.position
        val moveProject = element.moveProject ?: return
        val declaredNamedAddresses = moveProject.namedAddresses().values
        for ((name, addressVal) in declaredNamedAddresses.entries.sortedBy { it.key }) {
            val lookup = addressVal.createCompletionLookupElement(name)
            result.addElement(lookup)
        }
    }
}

fun TomlAddress.createCompletionLookupElement(lookupString: String): LookupElement {
    return LookupElementBuilder
        .create(lookupString)
        .withIcon(MoveIcons.ADDRESS)
        .withTypeText(packageName)
}
