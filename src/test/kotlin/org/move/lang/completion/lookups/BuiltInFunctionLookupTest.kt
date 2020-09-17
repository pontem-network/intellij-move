package org.move.lang.completion.lookups

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementPresentation
import org.move.lang.core.completion.createLookupElement
import org.move.lang.core.psi.MoveModuleDef
import org.move.lang.core.psi.ext.nativeFunctions
import org.move.utils.tests.MoveTestCase

class BuiltInFunctionLookupTest : MoveTestCase() {
    fun `test move_from`() = checkBuiltinPresentation(
        "move_from",
        tailText = "(_: address)",
        typeText = "R"
    )

    fun `test move_to`() = checkBuiltinPresentation(
        "move_to",
        tailText = "(_: address, _: R)",
        typeText = "()"
    )

    fun `test borrow_global`() = checkBuiltinPresentation(
        "borrow_global",
        tailText = "(_: address)",
        typeText = "&R"
    )

    fun `test borrow_global_mut`() = checkBuiltinPresentation(
        "borrow_global_mut",
        tailText = "(_: address)",
        typeText = "&mut R"
    )

    private fun checkBuiltinPresentation(name: String, tailText: String, typeText: String) {
        val moduleText = """
           module M {}
                //^
        """
        InlineFile(moduleText)
        val moduleElement = findElementInEditor<MoveModuleDef>()
        val lookup =
            moduleElement.nativeFunctions().find { it.name == name }!!.createLookupElement(false)
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