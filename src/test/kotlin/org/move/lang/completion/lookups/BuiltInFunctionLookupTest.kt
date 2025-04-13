package org.move.lang.completion.lookups

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementPresentation
import org.move.lang.core.completion.MvCompletionContext
import org.move.lang.core.completion.createCompletionItem
import org.move.lang.core.psi.MvModule
import org.move.lang.core.psi.builtinFunctions
import org.move.lang.core.resolve.scopeEntry.ScopeEntry
import org.move.lang.core.resolve.ref.NAMES
import org.move.lang.core.resolve.ref.Ns
import org.move.utils.tests.MvTestBase
import org.move.utils.tests.base.findElementInEditor

class BuiltInFunctionLookupTest: MvTestBase() {
    fun `test move_from`() = checkBuiltinPresentation(
        "move_from",
        tailText = "(addr: address): T",
        typeText = "builtins"
    )

    fun `test move_to`() = checkBuiltinPresentation(
        "move_to",
        tailText = "(acc: &signer, res: T)",
        typeText = "builtins"
    )

    fun `test borrow_global`() = checkBuiltinPresentation(
        "borrow_global",
        tailText = "(addr: address): &T",
        typeText = "builtins"
    )

    fun `test borrow_global_mut`() = checkBuiltinPresentation(
        "borrow_global_mut",
        tailText = "(addr: address): &mut T",
        typeText = "builtins"
    )

    private fun checkBuiltinPresentation(name: String, tailText: String, typeText: String) {
        val moduleText = """
           module 0x1::M {}
                     //^
        """
        InlineFile(moduleText)
        val moduleElement = myFixture.findElementInEditor<MvModule>()
        val lookup =
            moduleElement.builtinFunctions().single { it.name == name }.let {
                val scopeEntry = ScopeEntry(name, lazy { it }, Ns.NAME)
                val completionCtx = MvCompletionContext(it, false)
                createCompletionItem(scopeEntry, completionCtx)!!
            }
        checkLookupPresentation(
            lookup,
            tailText = tailText,
            typeText = typeText
        )
    }

    private fun checkLookupPresentation(
        lookup: LookupElement,
        tailText: String,
        typeText: String,
    ) {
        val presentation = LookupElementPresentation()
        lookup.renderElement(presentation)

        assertNotNull("Item icon should be not null", presentation.icon)
        assertEquals("Tail text mismatch", tailText, presentation.tailText)
        assertEquals("Type text mismatch", typeText, presentation.typeText)
    }
}
