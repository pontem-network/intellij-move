package org.move.lang.resolve

import org.intellij.lang.annotations.Language
import org.move.lang.core.psi.MvReferenceElement
import org.move.utils.tests.InlineFile
import org.move.utils.tests.base.findElementInEditor
import org.move.utils.tests.resolve.ResolveTestCase

class MultiResolveTest: ResolveTestCase() {
    fun `test struct literal shorthand`() = doTest("""
module 0x1::M {
    struct S { val: u8 }
    fun m() {
        let val = 1;
        S { val };
           //^
    }
}
    """)

    fun `test schema parameter shorthand`() = doTest("""
    module 0x1::M {
        spec module {
            let addr = @0x1;
            include MySchema { addr };
                                //^
        }
        
        spec schema MySchema {
            addr: address;
        }
    }    
    """)

    private fun doTest(@Language("Move") code: String) {
        InlineFile(myFixture, code, "main.move")
        val element = myFixture.findElementInEditor<MvReferenceElement>()
        val ref = element.reference ?: error("Failed to get reference for `${element.text}`")
        val variants = ref.multiResolve(false)
        check(variants.size == 2) {
            "Expected 2 variants, got $variants"
        }
    }
}
