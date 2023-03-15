package org.move.ide.annotator.errors

import org.move.ide.annotator.MvErrorAnnotator
import org.move.utils.tests.annotation.AnnotatorTestCase

class BuiltinCallErrorTest: AnnotatorTestCase(MvErrorAnnotator::class) {
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

    fun `test no error for the inline function`() = checkErrors("""
module 0x1::main {
    inline fun borrow_object<T: key>(source_object: &Object<T>): &T {
        borrow_global<T>(object::object_address(source_object))
    }
}        
    """)

//    fun `test outer function with acquiring inline function`() = checkErrors("""
//module 0x1::string {
//    struct String {}
//}
//module 0x1::main {
//    use 0x1::string::String;
//    fun call() {
//        borrow_object<String>(@0x1);
//    }
//    inline fun borrow_object<T: key>(addr: address): &T {
//        borrow_global<T>(addr)
//    }
//}
//    """)
}
