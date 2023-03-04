package org.move.lang.types

import org.move.utils.tests.types.TypificationTestCase

class ExpressionTypeInferenceTest: TypificationTestCase() {
    fun `test bool`() = testExpr("""
    module 0x1::M {
        fun main() {
            let b = false;
            b;
          //^ bool
        }
    }
    """)

    fun `test function parameter`() = testExpr("""
    module 0x1::M {
        fun main(a: u8) {
            let b = a;
            b;
          //^ u8  
        }
    }
    """)

    fun `test function call`() = testExpr(
        """
    module 0x1::M {
        fun call(): u8 { 1 }
        fun main() {
            let b = call();
            b;
          //^ u8  
        }
    }
    """
    )

    fun `test type parameter`() = testExpr("""
    module 0x1::M {
        fun main<F>(a: F) {
            let b = a;
            b;
          //^ F 
        }
    }    
    """)

    fun `test const type`() = testExpr("""
    module 0x1::M {
        const NUM: u8 = 1;
        
        fun main() {
            NUM;
          //^ u8  
        }
    }    
    """)

    fun `test function parameter constraint`() = testExpr("""
    module 0x1::M {
        fun main<Coin: copy + store>(coin: Coin) {
            coin;
          //^ Coin
        }
    }    
    """)

    fun `test tuple type unpacking`() = testExpr("""
    module 0x1::M {
        fun call(): (u8, u8) { (1, 1) }
        fun m() {
            let (a, b) = call();
            a; 
          //^ u8  
        }
    }    
    """)

    fun `test struct type unpacking`() = testExpr("""
    module 0x1::M {
        struct S { val: u8 }
        fun m() {
            let S { val: myval } = S { val: 1 };
            myval;
          //^ u8
        }
    }
    """)

    fun `test struct type unpacking shorthand`() = testExpr("""
    module 0x1::M {
        struct S { val: u8 }
        fun m() {
            let S { val } = S { val: 1 };
            val;
          //^ u8
        }
    }
    """)

    fun `test let inference in msl`() = testExpr("""
    module 0x1::M {
        spec module {
            let post a = 1;
            a;
          //^ num 
        }
    }    
    """)

    fun `test vector no specific type`() = testExpr("""
    module 0x1::M {
        native public fun vector_empty<Element>(): vector<Element>;
        native public fun vector_push_back<Element>(v: &mut vector<Element>, e: Element);
        fun call() {
            let v = vector_empty();
            v;
          //^ vector<?Element>  
        }
    }        
    """)

    fun `test vector inferred type`() = testExpr("""
    module 0x1::M {
        native public fun vector_empty<El>(): vector<El>;
        native public fun vector_push_back<Element>(v: &mut vector<Element>, e: Element);
        fun call() {
            let v = vector_empty();
            vector_push_back(&mut v, 1u8);
            v;
          //^ vector<u8>  
        }
    }        
    """)

    fun `test vector inferred type generic parameters`() = testExpr("""
    module 0x1::M {
        native public fun vector_empty<El>(): vector<El>;
        native public fun vector_push_back<Element>(v: &mut vector<Element>, e: Element);
        fun call() {
            let v = vector_empty();
            vector_push_back<u8>(&mut v);
            v;
          //^ vector<u8>  
        }
    }        
    """)

    fun `test infer type with struct literal`() = testExpr("""
    module 0x1::M {
        struct S { vec: vector<u8> }
        native public fun vector_empty<El>(): vector<El>;
        fun call() {
            let v = vector_empty();
            S { vec: v };
            v;
          //^ vector<u8>  
        }
    }        
    """)

    fun `test integer type inference from call expr`() = testExpr("""
    module 0x1::M {
        fun add_u8(a: u8, b: u8): u8 { a + b }
        fun call() {
            let a = 0;
            let b = 1;
            add_u8(a, b);
            a;
          //^ u8  
        }
    }    
    """)

    fun `test integer type inference with tail expr type`() = testExpr("""
    module 0x1::M {
        fun get_u8(): u8 {
            let a = 1;
            a
          //^ u8  
        }
    }    
    """)

    fun `test integer type inference with return expr type`() = testExpr("""
    module 0x1::M {
        fun get_u8(): u8 {
            let a = 1;
            a;
          //^ u8  
            return a
        }
    }    
    """)

    fun `test integer multi statement inside if expr block`() = testExpr("""
    module 0x1::M {
        fun put(i: u64, val: u64) {}
        fun main() {
            let i = 0;
            if (i < 10) {
                put(i, 10);
                i = i + 1;
            };
            i;
          //^ u64  
        }
    }    
    """)

    fun `test integer multi statement inside if while block`() = testExpr("""
    module 0x1::M {
        fun put(i: u64, val: u64) {}
        fun main() {
            let i = 0;
            while (i < 10) {
                put(i, 10);
                i = i + 1;
            };
            i;
          //^ u64  
        }
    }    
    """)

    fun `test integer multi statement inside loop block`() = testExpr("""
    module 0x1::M {
        fun put(i: u64, val: u64) {}
        fun main() {
            let i = 0;
            loop {
                put(i, 10);
                i = i + 1;
            };
            i;
          //^ u64  
        }
    }    
    """)

    fun `test integer multi statement inside else expr block`() = testExpr("""
    module 0x1::M {
        fun put(i: u64, val: u64) {}
        fun main() {
            let i = 0;
            if (i < 10) {
                i = i + 1;
            } else { 
                put(i, 10);
                i = i + 1;
            };
            i;
          //^ u64  
        }
    }    
    """)

    fun `test integer multi statement inside block expr`() = testExpr("""
    module 0x1::M {
        fun put(i: u64, val: u64) {}
        fun main() {
            let i = 0;
            {
                put(i, 10);
                i = i + 1;
            };
            i;
          //^ u64  
        }
    }    
    """)

    fun `test if block bindings are isolated from outside`() = testExpr("""
    module 0x1::M {
        fun main() {
            let i = 1u8;
            if (true) {
                let i = 1u64;
            };
            i;
          //^ u8 
        }
    }    
    """)

    fun `test transitive integer inference`() = testExpr("""
    module 0x1::M {
        fun identity<T>(a: T): T { a }
        fun main() {
            let a = 0;
            let b: u8 = identity(a);
            a;
          //^ u8  
        }
    }    
    """)

    fun `test struct unpacking type inference`() = testExpr("""
module 0x1::main {
    struct Container has key { val: u8 }
    fun main() {
        let container = move_from(source_addr);
        let Container { val } = container;
                                 //^ 0x1::main::Container        
    }
}                
    """)

    fun `tests struct unpacking incompatible field type`() = testExpr("""
module 0x1::main {
    struct Container<Type> { val: Type }
    fun main() {
        let Container<u8> { val } = Container<bool> { val: false };
        val;
      //^ u8  
    }
}        
    """)

    fun `test struct unpacking compatible field type inference`() = testExpr("""
module 0x1::main {
    struct Container<Type> { val: Type }
    fun main() {
        let Container { val } = Container<bool> { val: false };
        val;
      //^ bool  
    }
}        
    """)

    fun `test struct unpacking with explicit type`() = testExpr("""
module 0x1::main {
    struct Container<Type> { val: Type }
    fun main() {
        let Container { val }: Container<u8> = call();
        val;
      //^ u8
    }
}        
    """)

    fun `test integer type inference with boolean ops`() = testExpr("""
module 0x1::main {
    fun main(a: u64) {
        let c = 1;
        a < c;
        c;
      //^ u64  
    }
}        
    """)

    fun `test integer type inference with equal ops`() = testExpr("""
module 0x1::main {
    fun main(a: u64) {
        let c = 1;
        a == c;
        c;
      //^ u64  
    }
}        
    """)

    fun `test tablist map inference`() = testExpr(
        """
module 0x1::main {
    struct Tablist<
        phantom K: copy + drop + store,
        phantom V: store
    > has store {}
    
    struct OrderBook has store {}
    
    struct OrderBooks has key {
        map: Tablist<u64, OrderBook>
    }
    
    fun call() {
        let a = 1 + 1;
        a;
    }
    
    fun main() acquires OrderBooks {
        let order_books_map = &mut borrow_global_mut<OrderBooks>(@0x1).map;
        order_books_map;
        //^ &mut 0x1::main::Tablist<u64, 0x1::main::OrderBook>
    }
}        
    """
    )

    fun `test inference integer with msl block`() = testExpr("""
    module 0x1::mod {
        fun call() {
            let a = 1;
            spec {
                a = a + 1;
                a;
            };
            a;
          //^ integer  
        }
    }    
    """)

    fun `test lambda type type`() = testBinding(
        """
    module 0x1::mod {
        public inline fun fold<Accumulator, Element>(elem: Element, func: |Element| Accumulator): Accumulator {
                                                                    //^ |Element| Accumulator
        }
    }        
    """
    )

//    fun `test lambda expr type`() = testExpr("""
//    module 0x1::mod {
//        public inline fun fold<Accumulator, Element>(elem: Element, f: |Element| Accumulator): Accumulator {
//            let accum = f(elem);
//            accum;
//             //^ Accumulator
//        }
//    }
//    """)
//
//    fun `test lambda expr call expr inference`() = testExpr("""
//    module 0x1::mod {
//        public inline fun fold<Accumulator, Element>(elem: Element, f: |Element| Accumulator): Accumulator {
//            f(elem)
//        }
//        public fun main() {
//            let num = fold(1u64, |a| a);
//            num;
//            //^ u64
//        }
//    }
//    """)
//
//    fun `test lambda expr nested call expr inference`() = testExpr(
//        """
//    module 0x1::mod {
//        struct Option<Element> has copy, drop, store {
//            element: Element
//        }
//        public fun destroy_some<Element>(t: Option<Element>): Element {
//            t.element
//        }
//        public inline fun fold<Accumulator, Element>(
//            o: Option<Element>,
//            init: Accumulator,
//            f: |Accumulator,Element|Accumulator
//        ): Accumulator {
//            f(init, destroy_some(o))
//        }
//
//        fun main() {
//            let num = fold(Option { element: 1u64 }, 1, |a, b| a + b);
//            num;
//            //^ u64
//        }
//    }
//    """
//    )
}
