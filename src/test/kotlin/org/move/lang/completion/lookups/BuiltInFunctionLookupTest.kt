package org.move.lang.completion.lookups

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementPresentation
import org.move.lang.core.completion.createCompletionLookupElement
import org.move.lang.core.psi.MvModule
import org.move.lang.core.psi.ext.builtinFunctions
import org.move.utils.tests.MvTestBase
import org.move.utils.tests.base.findElementInEditor

class BuiltInFunctionLookupTest : MvTestBase() {
    fun `test move_from`() = checkBuiltinPresentation(
        "move_from",
        tailText = "(addr: address)",
        typeText = "T"
    )

    fun `test move_to`() = checkBuiltinPresentation(
        "move_to",
        tailText = "(acc: &signer, res: T)",
        typeText = "()"
    )

    fun `test borrow_global`() = checkBuiltinPresentation(
        "borrow_global",
        tailText = "(addr: address)",
        typeText = "&T"
    )

    fun `test borrow_global_mut`() = checkBuiltinPresentation(
        "borrow_global_mut",
        tailText = "(addr: address)",
        typeText = "&mut T"
    )

    private fun checkBuiltinPresentation(name: String, tailText: String, typeText: String) {
        val moduleText = """
           module M {}
                //^
        """
        inlineFile(moduleText)
        val moduleElement = myFixture.findElementInEditor<MvModule>()
        val lookup =
            moduleElement.builtinFunctions().find { it.name == name }!!.createCompletionLookupElement()
        checkLookupPresentation(
            lookup,
            tailText = tailText,
            typeText = typeText)
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
