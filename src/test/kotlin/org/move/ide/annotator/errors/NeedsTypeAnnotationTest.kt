package org.move.ide.annotator.errors

import org.move.ide.annotator.MvErrorAnnotator
import org.move.ide.inspections.MvTypeCheckInspection
import org.move.utils.tests.WithEnabledInspections
import org.move.utils.tests.annotation.AnnotatorTestCase

class NeedsTypeAnnotationTest: AnnotatorTestCase(MvErrorAnnotator::class) {

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

    fun `test explicit generic required for uninferrable type params`() = checkErrors("""
    module 0x1::M {
        fun call<R>() {}
        fun m() {
            <error descr="Could not infer this type. Try adding an annotation">call</error>();
        }
    }    
    """)

    @WithEnabledInspections(MvTypeCheckInspection::class)
    fun `test no needs type annotation error if type error happened in the child`() = checkErrors("""
        module 0x1::m {
            fun call<R>(a: u8, b: &R) {}
            fun main() {
                call(1, <error descr="Incompatible type 'bool', expected '&R'">false</error>);
            }
        }    
    """)

    fun `test no need type annotation error if function has missing value parameters`() = checkErrors("""
        module 0x1::m {
            fun call<R>(a: u8, b: &R) {}
            fun main() {
                call(<error descr="This function takes 2 parameters but 0 parameters were supplied">)</error>;
            }
        }    
    """)

    fun `test needs type annotation if missing params but not those do not affect inference`() = checkErrors("""
        module 0x1::m {
            fun call<R>(a: u8) {}
            fun main() {
                <error descr="Could not infer this type. Try adding an annotation">call</error>(<error descr="This function takes 1 parameter but 0 parameters were supplied">)</error>;
            }
        }    
    """)

    fun `test method type arguments inferrable`() = checkErrors("""
        module 0x1::main {
            struct S<T> { field: T }
            fun receiver<T, U>(self: &S<T>, param: U): U {
                param
            }
            fun main(s: S<u8>) {
                let a = s.receiver(1);
            }
        }        
    """)

    fun `test method type arguments uninferrable`() = checkErrors("""
        module 0x1::main {
            struct S { field: u8 }
            fun receiver<Z>(self: &S, param: u8): Z {
            }
            fun main(s: S) {
                let a = s.<error descr="Could not infer this type. Try adding an annotation">receiver</error>(1);
            }
        }        
    """)
}
