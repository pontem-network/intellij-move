package org.move.ide.annotator.errors

import org.move.ide.annotator.ErrorAnnotator
import org.move.utils.tests.annotation.AnnotatorTestCase

class InvalidReturnTypeTest: AnnotatorTestCase(ErrorAnnotator::class) {
    fun `test no return type but returns u8`() = checkErrors("""
    module M {
        fun call() {
            <error descr="Invalid return type: expected '()', found 'integer'">return 1</error>;
        }
    }    
    """)

    fun `test no return type but returns u8 with expression`() = checkErrors("""
    module M {
        fun call() {
            <error descr="Invalid return type: expected '()', found 'integer'">1</error>
        }
    }    
    """)

    fun `test if statement returns ()`() = checkErrors("""
    module M {
        fun m() {
            if (true) {1} else {2};
        }
    }    
    """)

    fun `test block expr returns ()`() = checkErrors("""
    module M {
        fun m() {
            {1};
        }
    }    
    """)

    fun `test error on code block if empty block and return type`() = checkErrors(
        """
    module M {
        fun call(): u8 {<error descr="Invalid return type: expected 'u8', found '()'">}</error>
    }    
        """
    )

    fun `test vector push back`() = checkErrors(
        """
    module 0x1::M {
        native public fun push_back<Element>(v: &mut vector<Element>, e: Element);
        
        fun m<E: drop>(v: &mut vector<E>, x: E): u8 {
            <error descr="Invalid return type: expected 'u8', found '()'">push_back(v, x)</error>
        }
    }    
    """
    )
}
