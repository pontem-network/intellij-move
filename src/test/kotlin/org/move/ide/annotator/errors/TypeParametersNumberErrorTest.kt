package org.move.ide.annotator.errors

import org.move.ide.annotator.ErrorAnnotator
import org.move.utils.tests.annotation.AnnotatorTestCase

class TypeParametersNumberErrorTest: AnnotatorTestCase(ErrorAnnotator::class) {
    fun `test missing type argument for vector`() = checkErrors("""
        module 0x1::M {
            fun main(val: <error descr="Missing item type argument">vector</error> ) {}
        }    
    """)

    fun `test too many type arguments for vector`() = checkErrors("""
        module 0x1::M {
            fun main(val: vector<
                u8, 
                <error descr="Wrong number of type arguments: expected 1, found 3">u8</error>, 
                <error descr="Wrong number of type arguments: expected 1, found 3">u8</error>> ) {}
        }    
    """)

    fun `test type params could be inferred for struct as type`() = checkErrors("""
        module 0x1::M {
            struct MyStruct<T> { field: T }
            
            fun main(val: MyStruct ) {}
        }    
    """)

    fun `test type params could be inferred for struct literal`() = checkErrors("""
        module 0x1::M {
            struct MyStruct<T> { field: T }
            
            fun main() {
                let a = MyStruct { field: 1 };
            }
        }    
    """)

    fun `test no type arguments expected`() = checkErrors("""
        module 0x1::M {
            struct MyStruct { field: u8 }
            
            fun main(val: MyStruct<error descr="No type arguments expected"><u8></error> ) {}
        }    
    """)

    fun `test resource could be inferred for move_to`() = checkErrors("""
        module 0x1::M {
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
        module 0x1::M {
            fun call<T>() {}
            fun main() {
                let a = call<u8, 
                    <error descr="Wrong number of type arguments: expected 1, found 3">u8</error>, 
                    <error descr="Wrong number of type arguments: expected 1, found 3">u8</error>>();
            }
        }    
    """)
}
