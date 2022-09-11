package org.move.ide.annotator.errors

import org.move.ide.annotator.ErrorAnnotator
import org.move.utils.tests.annotation.AnnotatorTestCase

class BuiltinCallErrorTest: AnnotatorTestCase(ErrorAnnotator::class) {
    fun `test builtin resource functions can only be called with struct from the current module`() = checkErrors("""
    module 0x1::M {
        struct S1 has key {}
    }   
    module 0x1::Main {
        use 0x1::M::S1;
        struct S2 has key {}
        fun m() {
            <error descr="The type '0x1::M::S1' was not declared in the current module. Global storage access is internal to the module">borrow_global_mut<S1></error>(@0x1);
            borrow_global_mut<S2>(@0x1);
        }
    }
    """)

    fun `test no error if type is unresolved`() = checkErrors("""
    module 0x1::Main {
        fun m() {
            borrow_global<S2>(@0x1);
        }
    }
    """)

    fun `test no global storage access error in spec`() = checkErrors("""
    module 0x1::M {
        struct S1 has key {}
    }   
    module 0x1::Main {
        use 0x1::M::S1;
        spec module {
            borrow_global_mut<S1>(@0x1);
        }
    }
    """)
}
