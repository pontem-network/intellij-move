package org.move.lang.completion.lookups

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementPresentation
import com.intellij.psi.NavigatablePsiElement
import org.intellij.lang.annotations.Language
import org.move.lang.core.completion.createCompletionLookupElement
import org.move.lang.core.psi.MvElement
import org.move.lang.core.psi.MvNamedElement
import org.move.utils.tests.MvTestBase
import org.move.utils.tests.base.findElementInEditor

class LookupElementTest : MvTestBase() {
    fun `test function param`() = check("""
        module M {
            fun call(a: u8) {
                   //^
            }
        }
    """, typeText = "u8")

    fun `test function`() = check("""
        module M {
            fun call(x: u64, account: &signer): u8 {}
              //^
        }
    """, tailText = "(x: u64, account: &signer)", typeText = "u8")

    fun `test multiline params function`() = check("""
        module M {
            fun call(x: u64, 
                     account: &signer): u8 {}
              //^
        }
    """, tailText = "(x: u64, account: &signer)", typeText = "u8")

    fun `test const item`() = check("""
        module M {
            const MY_CONST: u8 = 1;
                //^
        }
    """, typeText = "u8")

    fun `test struct`() = check("""
        module M {
            struct MyStruct { val: u8 }
                 //^
        }
    """, tailText = " { ... }")

    fun `test module`() = check("""
        address 0x1 {
            module M {}
                //^
        }
    """, typeText = "main.move")

//    fun `test define`() = check("""
//        module M {
//            spec module {
//                define renamed_reserve_exists(val: u8): bool {}
//                     //^
//            }
//        }
//    """, tailText = "(val: u8)", typeText = "bool")

    private fun check(
        @Language("Move") code: String,
        tailText: String? = null,
        typeText: String? = null,
        isBold: Boolean = false,
        isStrikeout: Boolean = false,
    ) = checkInner<MvNamedElement>(code, tailText, typeText, isBold, isStrikeout)

    private inline fun <reified T> checkInner(
        @Language("Move") code: String,
        tailText: String? = null,
        typeText: String? = null,
        isBold: Boolean = false,
        isStrikeout: Boolean = false,
    ) where T : NavigatablePsiElement, T : MvElement {
        inlineFile(code)

        val element = myFixture.findElementInEditor<T>() as? MvNamedElement
            ?: error("Marker `^` should point to the MvNamedElement")

        val lookup = element.createCompletionLookupElement()
        checkLookupPresentation(
            lookup,
            tailText = tailText,
            typeText = typeText,
            isBold = isBold,
            isStrikeout = isStrikeout
        )
    }

    private fun checkLookupPresentation(
        lookup: LookupElement,
        tailText: String?,
        typeText: String?,
        isBold: Boolean,
        isStrikeout: Boolean,
    ) {
        val presentation = LookupElementPresentation()
        lookup.renderElement(presentation)

        assertNotNull("Item icon should be not null", presentation.icon)
        assertEquals("Tail text mismatch", tailText, presentation.tailText)
        assertEquals("Type text mismatch", typeText, presentation.typeText)
        assertEquals("Bold text attribute mismatch", isBold, presentation.isItemTextBold)
        assertEquals("Strikeout text attribute mismatch", isStrikeout, presentation.isStrikeout)
    }
}
