package org.move.ide.annotator.errors

import org.move.ide.annotator.MvErrorAnnotator
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
                let a: <error descr="Invalid instantiation of '0x1::M::MyStruct'. Expected 0 type argument(s) but got 1">MyStruct<u8></error>;
            }
        }    
    """)

    fun `test no type arguments expected for imported alias`() = checkErrors("""
        module 0x1::string {
            struct MyStruct { field: u8 }
        }
        module 0x1::M {
            use 0x1::string::MyStruct as Struct;            
            fun m() {
                let a: <error descr="Invalid instantiation of '0x1::string::MyStruct'. Expected 0 type argument(s) but got 1">Struct<u8></error>;
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
                let a = <error descr="Invalid instantiation of '0x1::M::call'. Expected 0 type argument(s) but got 1">call<u8></error>();
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
                let a = <error descr="Invalid instantiation of '0x1::M::call'. Expected 1 type argument(s) but got 2">call<u8, u8></error>();
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
        struct Event { val: <error descr="Invalid instantiation of '0x1::M::S'. Expected 1 type argument(s) but got 2">S<u8, u8></error> }
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
            let a = <error descr="Invalid instantiation of '0x1::M::S'. Expected 2 type argument(s) but got 1">S<u8></error> {};
        }
    }    
    """)

    fun `test explicit generic required for uninferrable type params`() = checkErrors("""
    module 0x1::M {
        fun call<R>() {}
        fun m() {
            <error descr="Could not infer this type. Try adding an annotation">call</error>();
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
            include <error descr="Invalid instantiation of '0x1::M::MySchema'. Expected 2 type argument(s) but got 1">MySchema<u8></error>;
        }
    }    
    """)

    fun `test no error if schema params are inferrable`() = checkErrors("""
    module 0x1::M {
        struct Token<Type> {}
        spec schema MySchema<Type> {
            token: Token<Type>;
        }
        fun call() {}
        spec call {
            include MySchema { token: Token<u8> };
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

    fun `test no need for explicit type infer from return type`() = checkErrors(
        """
        module 0x1::m {
            struct Coin<CoinType> { val: u8 }
            struct S<X> { coins: Coin<X> }
            struct BTC {}
            fun coin_zero<ZeroCoinType>(): Coin<ZeroCoinType> { Coin<ZeroCoinType> { val: 0 } }
            fun call<CallCoinType>() {
                S<CallCoinType> { coins: coin_zero() };
            }
        }        
    """
    )

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

    fun `test type parameter can be inferred from mut vector ref`() = checkErrors("""
        module 0x1::m {
            fun swap<T>(v: &mut vector<T>) {
                swap(v);
            }
        }        
    """)

    fun `test no type annotation error if name is unresolved but type is inferrable`() = checkErrors("""
        module 0x1::m {
            struct Option<Element> has copy, drop, store {
                vec: vector<Element>
            }
            native fun is_none<Element>(t: &Option<Element>): bool;
            fun main() {
                is_none(unknown_name);
            }
        }        
    """)

    fun `test no type annotation error if return type is unresolved but type is inferrable`() = checkErrors("""
        module 0x1::m {
            struct Option<Element> has copy, drop, store {
                vec: vector<Element>
            }
            native fun none<Element>(): Option<Element>;
            fun main() {
                none() == unknown_name;
            }
        }        
    """)

    fun `test no needs type annotation for spec struct field item passed`() = checkErrors("""
        module 0x1::m {
            struct Option<Element> has copy, drop, store {
                vec: vector<Element>
            }
            native fun is_none<Element>(t: &Option<Element>): bool;
            struct S { aggregator: Option<u8> }
            spec S {
                is_none(aggregator);
            }
        }        
    """)
}
