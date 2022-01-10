package org.move.ide.annotator.errors

import org.move.ide.annotator.ErrorAnnotator
import org.move.utils.tests.annotation.AnnotatorTestCase

class TypeParametersNumberErrorTest: AnnotatorTestCase(ErrorAnnotator::class) {
    fun `test missing type argument for vector`() = checkErrors("""
        module M {
            fun main(val: <error descr="Missing item type argument">vector</error> ) {}
        }    
    """)

    fun `test too many type arguments for vector`() = checkErrors("""
        module M {
            fun main(val: vector<
                u8, 
                <error descr="Wrong number of type arguments: expected 1, found 3">u8</error>, 
                <error descr="Wrong number of type arguments: expected 1, found 3">u8</error>> ) {}
        }    
    """)

    fun `test type params could be inferred for struct as type`() = checkErrors("""
        module M {
            struct MyStruct<T> { field: T }
            
            fun main(val: MyStruct ) {}
        }    
    """)

    fun `test type params could be inferred for struct literal`() = checkErrors("""
        module M {
            struct MyStruct<T> { field: T }
            
            fun main() {
                let a = MyStruct { field: 1 };
            }
        }    
    """)

    fun `test no type arguments expected`() = checkErrors("""
        module M {
            struct MyStruct { field: u8 }
            
            fun main(val: MyStruct<error descr="No type arguments expected"><u8></error> ) {}
        }    
    """)

//    fun `test missing resource argument for builtins`() = checkErrors("""
//        module M {
//            fun main() {
//                let a = <error descr="Missing resource type argument">move_from</error>(@0x0);
//                let a = <error descr="Missing resource type argument">borrow_global</error>(@0x0);
//                let a = <error descr="Missing resource type argument">borrow_global_mut</error>(@0x0);
//                let a = <error descr="Missing resource type argument">exists</error>(@0x0);
//            }
//        }
//    """)

    fun `test resource could be inferred for move_to`() = checkErrors("""
        module M {
            fun main(s: signer) {
                let a = move_to(&s, 1);
            }
        }    
    """)

    fun `test no type arguments expected for function`() = checkErrors("""
        module 0x1::M {
            fun call() {}
            fun main() {
                let a = call<error descr="No type arguments expected"><u8></error>();
            }
        }    
    """)

    fun `test generic argument type could be inferred`() = checkErrors("""
        module 0x1::M {
            fun call<T>(val: T) {}
            fun main() {
                let a = call(1);
            }
        }    
    """)

    fun `test too many type arguments for function`() = checkErrors("""
        module M {
            fun call<T>() {}
            fun main() {
                let a = call<u8, 
                    <error descr="Wrong number of type arguments: expected 1, found 3">u8</error>, 
                    <error descr="Wrong number of type arguments: expected 1, found 3">u8</error>>();
            }
        }    
    """)
}
