package org.move.lang.completion.lookups

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementPresentation
import org.move.lang.core.completion.createLookupElement
import org.move.lang.core.psi.MoveModuleDef
import org.move.lang.core.psi.ext.builtinFunctions
import org.move.utils.tests.MoveTestBase

class BuiltInFunctionLookupTest : MoveTestBase() {
    fun `test move_from`() = checkBuiltinPresentation(
        "move_from",
        tailText = "(addr: address)",
        typeText = "R"
    )

    fun `test move_to`() = checkBuiltinPresentation(
        "move_to",
        tailText = "(addr: address, res: R)",
        typeText = "()"
    )

    fun `test borrow_global`() = checkBuiltinPresentation(
        "borrow_global",
        tailText = "(addr: address)",
        typeText = "&R"
    )

    fun `test borrow_global_mut`() = checkBuiltinPresentation(
        "borrow_global_mut",
        tailText = "(addr: address)",
        typeText = "&mut R"
    )

    private fun checkBuiltinPresentation(name: String, tailText: String, typeText: String) {
        val moduleText = """
           module M {}
                //^
        """
        inlineFile(moduleText)
        val moduleElement = findElementInEditor<MoveModuleDef>()
        val lookup =
            moduleElement.builtinFunctions().find { it.name == name }!!.createLookupElement(false)
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
