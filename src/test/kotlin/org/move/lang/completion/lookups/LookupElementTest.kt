package org.move.lang.completion.lookups

import com.intellij.codeInsight.completion.CompletionContext
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionSorter
import com.intellij.codeInsight.completion.PrefixMatcher
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementPresentation
import com.intellij.patterns.ElementPattern
import com.intellij.psi.NavigatablePsiElement
import org.intellij.lang.annotations.Language
import org.move.lang.core.completion.MvCompletionContext
import org.move.lang.core.completion.createLookupElement
import org.move.lang.core.completion.providers.CommonCompletionProvider
import org.move.lang.core.psi.MvElement
import org.move.lang.core.psi.MvNamedElement
import org.move.lang.core.psi.ext.MvMethodOrField
import org.move.lang.core.psi.ext.isMsl
import org.move.lang.core.resolve.SimpleScopeEntry
import org.move.lang.core.resolve.ref.MvReferenceElement
import org.move.lang.core.resolve.ref.NAMES
import org.move.utils.tests.MoveV2
import org.move.utils.tests.MvTestBase
import org.move.utils.tests.base.findElementInEditor

class LookupElementTest: MvTestBase() {
    fun `test function param`() = checkNamedItem(
        """
        module 0x1::M {
            fun call(a: u8) {
                   //^
            }
        }
    """, typeText = "u8"
    )

    fun `test function`() = checkNamedItem(
        """
        module 0x1::M {
            fun call(x: u64, account: &signer): u8 {}
              //^
        }
    """, tailText = "(x: u64, account: &signer): u8", typeText = "main.move"
    )

    fun `test multiline params function`() = checkNamedItem(
        """
        module 0x1::M {
            fun call(x: u64, 
                     account: &signer): u8 {}
              //^
        }
    """, tailText = "(x: u64, account: &signer): u8", typeText = "main.move"
    )

    fun `test const item`() = checkNamedItem(
        """
        module 0x1::M {
            const MY_CONST: u8 = 1;
                //^
        }
    """, typeText = "u8"
    )

    fun `test struct`() = checkNamedItem(
        """
        module 0x1::M {
            struct MyStruct { val: u8 }
                 //^
        }
    """, typeText = "main.move"
    )

    fun `test module`() = checkNamedItem(
        """
        address 0x1 {
            module M {}
                //^
        }
    """, tailText = " 0x1", typeText = "main.move"
    )

    fun `test module with named address`() = checkNamedItem(
        """
        module Std::M {}
                  //^
    """, tailText = " Std", typeText = "main.move"
    )

    fun `test generic field`() = checkMethodOrFieldProvider(
        """
        module 0x1::main {
            struct S<T> { field: T }
            fun main() {
                let s = S { field: 1u8 };
                s.field;
                  //^ 
            }
        }        
    """, typeText = "u8"
    )

    @MoveV2(enabled = false)
    fun `test self method without receiver style enabled`() = checkNamedItem(
        """
        module 0x1::main {
            struct S<T> { field: T }
            fun receiver<T>(self: S<T>): T {}
                    //^ 
            fun main() {
                let s = S { field: 1u8 };
                receiver(s);
            }
        }        
    """, tailText = "(self: S<T>): T", typeText = "main.move"
    )

    @MoveV2()
    fun `test generic method`() = checkMethodOrFieldProvider(
        """
        module 0x1::main {
            struct S<T> { field: T }
            fun receiver<T>(self: S<T>): T {}
            fun main() {
                let s = S { field: 1u8 };
                s.receiver();
                  //^ 
            }
        }        
    """, tailText = "(self)", typeText = "u8"
    )

    @MoveV2()
    fun `test generic method ref`() = checkMethodOrFieldProvider(
        """
        module 0x1::main {
            struct S<T> { field: T }
            fun receiver<T>(self: &S<T>): T {}
            fun main() {
                let s = S { field: 1u8 };
                s.receiver();
                  //^ 
            }
        }        
    """, tailText = "(&self)", typeText = "u8"
    )

    @MoveV2()
    fun `test generic method ref mut`() = checkMethodOrFieldProvider(
        """
        module 0x1::main {
            struct S<T> { field: T }
            fun receiver<T>(self: &mut S<T>): T {}
            fun main() {
                let s = S { field: 1u8 };
                s.receiver();
                  //^ 
            }
        }        
    """, tailText = "(&mut self)", typeText = "u8"
    )

    fun `test named tuple field lookup`() = checkMethodOrFieldProvider(
        """
        module 0x1::m {
            struct S(u8, u8);
            fun main(s: S) {
                s.0;
                //^
            }
        }
        """, typeText = "u8"
    )

    fun `test lookup for tuple struct`() = checkNamedItem("""
        module 0x1::m {
            struct SS(u8, u16);
                 //^ 
        }        
    """, tailText = "(u8, u16)", typeText = "main.move")

    private fun checkNamedItem(
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
    ) where T: NavigatablePsiElement, T: MvElement {
        InlineFile(code)

        val element = myFixture.findElementInEditor<T>() as? MvNamedElement
            ?: error("Marker `^` should point to the MvNamedElement")

        val name = element.name ?: error("name == null")
        val scopeEntry = SimpleScopeEntry(name, element, NAMES)
        val completionCtx = MvCompletionContext(element, false)

        val lookup = createLookupElement(scopeEntry, completionCtx)
        checkLookupPresentation(
            lookup,
            tailText = tailText,
            typeText = typeText,
            isBold = isBold,
            isStrikeout = isStrikeout
        )
    }

    private fun checkMethodOrFieldProvider(
        @Language("Move") code: String,
        tailText: String? = null,
        typeText: String? = null,
        isBold: Boolean = false,
        isStrikeout: Boolean = false
    ) {
        InlineFile(code)
        val element = findElementInEditor<MvReferenceElement>()

        val lookups = mutableListOf<LookupElement>()
        val result = object: CompletionResultSet(PrefixMatcher.ALWAYS_TRUE, null, null) {
            override fun caseInsensitive(): CompletionResultSet = this
            override fun withPrefixMatcher(matcher: PrefixMatcher): CompletionResultSet = this
            override fun withPrefixMatcher(prefix: String): CompletionResultSet = this
            override fun restartCompletionOnPrefixChange(prefixCondition: ElementPattern<String>?) {}
            override fun addLookupAdvertisement(text: String) {}
            override fun withRelevanceSorter(sorter: CompletionSorter): CompletionResultSet = this
            override fun restartCompletionWhenNothingMatches() {}
            override fun addElement(element: LookupElement) {
                lookups.add(element)
            }
        }

        if (element is MvMethodOrField) {
            val ctx = MvCompletionContext(element, element.isMsl())
            CommonCompletionProvider.addMethodOrFieldVariants(element, result, ctx)
        }

        val lookup = lookups.single {
            val namedElement = it.psiElement as? MvNamedElement
            namedElement?.name == element.referenceName
        }
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
