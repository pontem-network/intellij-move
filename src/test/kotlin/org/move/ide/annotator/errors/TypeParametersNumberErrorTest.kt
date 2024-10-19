package org.move.ide.annotator.errors

import org.move.ide.annotator.MvErrorAnnotator
import org.move.utils.tests.MoveV2
import org.move.utils.tests.annotation.AnnotatorTestCase

class TypeParametersNumberErrorTest: AnnotatorTestCase(MvErrorAnnotator::class) {
    fun `test missing type argument for vector`() = checkErrors("""
        module 0x1::M {
            fun m() {
            let a: vector<address>;
            let b: <error descr="Invalid instantiation of 'vector'. Expected 1 type argument(s) but got 0">vector</error>;
            let c: <error descr="Invalid instantiation of 'vector'. Expected 1 type argument(s) but got 3">vector<u8, u8, u8></error>;
            }
            
            #[test(location = std::vector)]
            fun test_a() {
                
            }
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
            
            fun m() {
                let a: MyStruct<error descr="No type arguments expected for '0x1::M::MyStruct'"><u8></error>;
            }
        }    
    """)

    fun `test no type arguments expected for imported alias`() = checkErrors("""
        module 0x1::m {
            struct MyStruct { field: u8 }
        }
        module 0x1::main {
            use 0x1::m::MyStruct as Struct;            
            fun main() {
                let a: Struct<error descr="No type arguments expected for '0x1::m::MyStruct'"><u8></error>;
            }
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
                let a = call<error descr="No type arguments expected for '0x1::M::call'"><u8></error>();
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
                let a = call<u8, <error descr="Invalid instantiation of '0x1::M::call'. Expected 1 type argument(s) but got 2">u8</error>>();
            }
        }    
    """)

    fun `test missing generic params for type`() = checkErrors("""
    module 0x1::M {
        struct S<R> { r: R }
        struct Event { val: <error descr="Invalid instantiation of '0x1::M::S'. Expected 1 type argument(s) but got 0">S</error> }
    }    
    """)

    fun `test too many generic params for type`() = checkErrors("""
    module 0x1::M {
        struct S<R> { r: R }
        struct Event { val: S<u8, <error descr="Invalid instantiation of '0x1::M::S'. Expected 1 type argument(s) but got 2">u8</error>> }
    }    
    """)

    fun `test no need for generic parameters inside acquires`() = checkErrors(
        """
    module 0x1::M {
        struct S<phantom R> has key {}
        fun m() acquires S {
            borrow_global_mut<S<u8>>(@0x1);
        }
    }    
    """
    )

    fun `test wrong number of type params for struct`() = checkErrors("""
    module 0x1::M {
        struct S<R, RR> {}
        fun m() {
            let a = S<error descr="Invalid instantiation of '0x1::M::S'. Expected 2 type argument(s) but got 1"><u8></error> {};
        }
    }    
    """)

    fun `test phantom type can be inferred from explicitly passed generic`() = checkErrors("""
    module 0x1::M {
        struct CapState<phantom Feature> has key {}
        fun m<Feature>(acc: &signer) {
            move_to<CapState<Feature>>(acc, CapState{})
        }
    }    
    """)

    fun `test phantom type can be inferred from another struct with phantom type`() = checkErrors("""
    module 0x1::M {
        struct Slot<phantom Feature> has store {}
        struct Container<phantom Feature> has key { slot: Slot<Feature> }
        fun m<Feature>(acc: &signer) {
            Container{ slot: Slot<Feature> {} };
        }
    }    
    """)

    fun `test not enough type params for schema`() = checkErrors("""
    module 0x1::M {
        spec schema MySchema<Type1, Type2> {}
        fun call() {}
        spec call {
            include MySchema<error descr="Invalid instantiation of '0x1::M::MySchema'. Expected 2 type argument(s) but got 1"><u8></error>;
        }
    }    
    """)

    fun `test missing type params if uninferrable`() = checkErrors("""
    module 0x1::M {
        spec schema MySchema<Type> {}
        fun call() {}
        spec call {
            include <error descr="Invalid instantiation of '0x1::M::MySchema'. Expected 1 type argument(s) but got 0">MySchema</error>;
        }
    }        
    """)

    fun `test binding receives type in the separate block`() = checkErrors("""
        module 0x1::m {
            struct Option<phantom Element> {}
            fun some<Element>(m: Element): Option<Element> {}
            fun none<Element>(): Option<Element> {}
            fun main() {
                let (opt1, opt2);
                if (true) {
                    (opt1, opt2) = (some(1u8), some(1u8));
                } else {
                    (opt1, opt2) = (none(), none());
                }
            }
        }        
    """)

    @MoveV2()
    fun `test receiver style method missing type parameter`() = checkErrors("""
        module 0x1::main {
            struct S<T> { field: T }
            fun receiver<T, U>(self: &S<T>, param: U): U {
                param
            }
            fun main(s: S<u8>) {
                let b = s.receiver<error descr="Invalid instantiation of '0x1::main::receiver'. Expected 2 type argument(s) but got 1">::<u8></error>(1);
            }
        }        
    """)


    fun `test no error for vector in fq expr if unresolved`() = checkErrors("""
        module 0x1::m {
            fun main() {
                vector::push_back();
            }
        }        
    """)

    fun `test no error for vector in local path expr`() = checkErrors("""
        module 0x1::m {
            fun main() {
                vector;
            }
        }        
    """)

    fun `test no error for vector in type position`() = checkErrors("""
        module 0x1::m {
            fun main(s: vector::Vector) {
            }
        }        
    """)

    fun `test no error for vector in type position with qualifier`() = checkErrors("""
        module 0x1::m {
            fun main(s: std::vector) {
            }
        }        
    """)

    fun `test vector type position arguments error in presence of vector module`() = checkErrors("""
        module 0x1::vector {}
        module 0x1::m {
            use 0x1::vector;
            fun main(s: <error descr="Invalid instantiation of 'vector'. Expected 1 type argument(s) but got 0">vector</error>) {}
        }
    """)
}
