package org.move.lang.completion.lookups

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementPresentation
import org.move.lang.core.completion.MvCompletionContext
import org.move.lang.core.completion.createLookupElement
import org.move.lang.core.psi.MvModule
import org.move.lang.core.psi.ext.builtinFunctions
import org.move.lang.core.resolve.SimpleScopeEntry
import org.move.lang.core.resolve.ref.NAMES
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
                val scopeEntry = SimpleScopeEntry(name, it, NAMES)
                val completionCtx = MvCompletionContext(it, false)
                createLookupElement(scopeEntry, completionCtx)
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
