package org.move.lang.types

import org.move.utils.tests.types.TypificationTestCase

class ExpressionTypesTest: TypificationTestCase() {
    fun `test add expr`() = testExpr("""
    script {
        fun main() {
            (1u8 + 1u8);
          //^ u8
        }
    }    
    """)

    fun `test sub expr`() = testExpr("""
    script {
        fun main() {
            (1u8 - 1u8);
          //^ u8
        }
    }    
    """)

    fun `test mul expr`() = testExpr("""
    script {
        fun main() {
            (1u8 * 1u8);
          //^ u8
        }
    }    
    """)

    fun `test div expr`() = testExpr("""
    script {
        fun main() {
            (1u8 / 1u8);
          //^ u8
        }
    }    
    """)

    fun `test mod expr`() = testExpr("""
    script {
        fun main() {
            (1u8 % 10);
          //^ u8
        }
    }    
    """)

    fun `test bang expr`() = testExpr("""
    script {
        fun main() {
            !true;
          //^ bool
        }
    }    
    """)

    fun `test less expr`() = testExpr("""
    script {
        fun main() {
            (1 < 1);
          //^ bool
        }
    }    
    """)

    fun `test less equal expr`() = testExpr("""
    script {
        fun main() {
            (1 <= 1);
          //^ bool
        }
    }    
    """)

    fun `test greater expr`() = testExpr("""
    script {
        fun main() {
            (1 > 1);
          //^ bool
        }
    }    
    """)

    fun `test greater equal expr`() = testExpr("""
    script {
        fun main() {
            (1 >= 1);
          //^ bool
        }
    }    
    """)

    fun `test cast expr`() = testExpr("""
    script {
        fun main() {
            (1 as u8);
          //^ u8
        }
    }    
    """)

    fun `test copy expr`() = testExpr("""
    script {
        fun main() {
            copy 1u8;
          //^ u8
        }
    }    
    """)

    fun `test move expr`() = testExpr("""
    script {
        fun main() {
            move 1u8;
          //^ u8
        }
    }    
    """)

    fun `test struct literal expr with unresolved type param`() = testExpr("""
    module 0x1::M {
        struct R<CoinType> {}
        fun main() {
            R {};
          //^ 0x1::M::R<<unknown>>  
        }
    }
    """)

    fun `test borrow expr`() = testExpr("""
    module 0x1::M {
        fun main(s: signer) {
            &s;
          //^ &signer 
        }
    }    
    """)

    fun `test mutable borrow expr`() = testExpr("""
    module 0x1::M {
        fun main(s: signer) {
            &mut s;
          //^ &mut signer 
        }
    }    
    """)

    fun `test deref expr`() = testExpr("""
    module 0x1::M {
        fun main(s: &signer) {
            *s;
          //^ signer 
        }
    }    
    """)

    fun `test dot access to primitive field`() = testExpr("""
    module 0x1::M {
        struct S { addr: address }
        fun main() {
            let s = S { addr: @0x1 };
            ((&s).addr);
          //^ address 
        }
    }    
    """)

    fun `test dot access to field with struct type`() = testExpr("""
    module 0x1::M {
        struct Addr {}
        struct S { addr: Addr }
        fun main() {
            let s = S { addr: Addr {} };
            ((&s).addr);
          //^ 0x1::M::Addr 
        }
    }    
    """)

    fun `test borrow expr of dot access`() = testExpr("""
    module 0x1::M {
        struct Addr {}
        struct S { addr: Addr }
        fun main() {
            let s = S { addr: Addr {} };
            &mut s.addr;
          //^ &mut 0x1::M::Addr 
        }
    }    
    """)

    fun `test add expr with untyped and typed integer`() = testExpr("""
    module 0x1::M {
        fun main() {
            (1 + 1u8);
          //^ u8  
        }
    }    
    """)

    fun `test add expr with untyped and typed integer reversed`() = testExpr("""
    module 0x1::M {
        fun main() {
            (1u8 + 1);
          //^ u8  
        }
    }    
    """)

    fun `test struct field as vector`() = testExpr("""
    module 0x1::M {
        struct NFT {}
        struct Collection { nfts: vector<NFT> }
        fun m(coll: Collection) {
            (coll.nfts);
          //^ vector<0x1::M::NFT>  
        }
    }    
    """)

    fun `test if expr`() = testExpr("""
    module 0x1::M {
        fun m() {
            let a = if (true) 1 else 2;
            a;
          //^ integer 
        }
    }    
    """)

    fun `test if expr without else`() = testExpr("""
    module 0x1::M {
        fun m() {
            let a = if (true) 1;
            a;
          //^ () 
        }
    }    
    """)

    fun `test if expr with incompatible else`() = testExpr("""
    module 0x1::M {
        fun m() {
            let a = if (true) 1 else true;
            a;
          //^ <unknown>
        }
    }    
    """)

    fun `test return type of unit returning function`() = testExpr("""
    module 0x1::M {
        fun call(): () {}
        fun m() {
            call();
          //^ ()
        }
    }    
    """)

    fun `test if else with references coerced to less specific one`() = testExpr("""
    module 0x1::M {
        struct S {}
        fun m(s: &S, s_mut: &mut S) {
            let cond = true;
            (if (cond) s_mut else s);
          //^ &0x1::M::S  
        }
    }    
    """)

    fun `test x string`() = testExpr("""
    module 0x1::M {
        fun m() {
            x"1234";
            //^ vector<u8>
        }
    }    
    """)

    fun `test msl num`() = testExpr("""
    module 0x1::M {
        spec module {
            1;
          //^ num  
        }
    }    
    """)

    fun `test msl callable num`() = testExpr("""
    module 0x1::M {
        fun call(): u8 { 1 }
        spec module {
            call();
            //^ num
        }
    }    
    """)

    fun `test msl ref is type`() = testExpr("""
    module 0x1::M {
        struct S {}
        fun ref(): &S {}
        spec module {
            let a = ref();
            a;
          //^ 0x1::M::S
        }
    }    
    """)

    fun `test msl mut ref is type`() = testExpr("""
    module 0x1::M {
        struct S {}
        fun ref_mut(): &mut S {}
        spec module {
            let a = ref_mut();
            a;
          //^ 0x1::M::S
        }
    }    
    """)

    fun `test type of fun param in spec`() = testExpr("""
    module 0x1::M {
        fun call(addr: address) {}
        spec call {
            addr;
            //^ address
        }
    }    
    """)

    fun `test type of u8 fun param in spec`() = testExpr("""
    module 0x1::M {
        fun call(n: u8) {}
        spec call {
            n;
          //^ num  
        }
    }    
    """)

//    fun `test type of result variable in fun spec is return type`() = testExpr("""
//    module 0x1::M {
//        fun call(): address { @0x1 }
//        spec call {
//            result;
//            //^ address
//        }
//    }
//    """)

    fun `test old function type for spec`() = testExpr("""
    module 0x1::M {
        struct S {}
        fun call(a: S) {}
        spec call {
            old(a);
          //^ 0x1::M::S 
        }
    }    
    """)

    fun `test global function type for spec`() = testExpr("""
    module 0x1::M {
        struct S has key {}
        spec module {
            let a = global<S>(@0x1);
            a;
          //^ 0x1::M::S 
        }
    }    
    """)

    fun `test const int in spec`() = testExpr("""
    module 0x1::M {
        const MY_INT: u8 = 1;
        spec module {
            MY_INT;
            //^ num
        }
    }    
    """)

    fun `test schema field type`() = testExpr("""
    module 0x1::M {
        spec schema SS {
            val: num;
            val;
            //^ num
        }
    }    
    """)

    fun `test struct field vector_u8 in spec`() = testExpr("""
    module 0x1::M {
        struct S { vec: vector<u8> } 
        spec module {
            let s = S { vec: b"" };
            s.vec
            //^ vector<num>
        }
    }    
    """)

    fun `test tuple type`() = testExpr("""
    module 0x1::M {
        fun m() {
            (1u64, 1u64);
          //^ (u64, u64)  
        }
    }    
    """)

    fun `test explicit generic type struct`() = testExpr("""
    module 0x1::M {
        struct Option<Element> {}
        fun call() {
            let a = Option<u8> {};
            a;
          //^ 0x1::M::Option<u8>  
        }
    }        
    """)

    fun `test type of plus with invalid arguments`() = testExpr("""
    module 0x1::M {
        fun add(a: bool, b: bool) {
            (a + b);
          //^ <unknown>  
        }
    }    
    """)

    fun `test struct lit with generic of type with incorrect abilities`() = testExpr("""
    module 0x1::M {
        struct S<phantom Message: store> {}
        struct R has copy {  }
        fun main() {
            S<R> {};
          //^ 0x1::M::S<0x1::M::R>  
        }
    }    
    """)

    fun `test while expr returns unit`() = testExpr("""
    module 0x1::M {
        fun main() {
            let a = while (true) { 1; };
            a;
          //^ ()  
        }
    }    
    """)

    fun `test return value from block`() = testExpr("""
    module 0x1::M {
        fun main() {
            let a = { 1u8 };
            a;
          //^ u8  
        }
    }    
    """)

    fun `test if else return`() = testExpr("""
    module 0x1::M {
        fun main(): u8 {
            if (true) { return 1 } else { return 2 }
          //^ <never>  
        }
    }    
    """)

    fun `test unpack struct into field`() = testExpr("""
        module 0x1::M {
        struct S { val: u8 }
        fun s(): S { S { val: 10 } }
        fun main() {
            let s = s();
            s;
          //^ 0x1::M::S   
        }
    }            
    """)

    fun `test unpack tuple of structs`() = testExpr("""
        module 0x1::M {
        struct S { val: u8 }
        fun s(): (S, S) { (S { val: 10 }, S { val: 10 }) }
        fun main() {
            let (s, t) = s();
            s;
          //^ 0x1::M::S   
        }
    }            
    """)

    fun `test integer inference with spec blocks inside block`() = testExpr("""
    module 0x1::main {
        spec fun get_num(): num { 1 }
        fun main() {
            let myint = 1;
            myint + 1u8;
            spec {
                myint
                //^ num
            };
        }
    }    
    """)

    fun `test integer inference with spec blocks outside block`() = testExpr("""
    module 0x1::main {
        spec fun get_num(): num { 1 }
        fun main() {
            let myint = 1;
            myint + 1u8;
            spec {
                myint + get_num();
            };
            myint;
            //^ u8
        }
    }    
    """)

    fun `test vector lit with explicit type`() = testExpr("""
    module 0x1::main {
        fun main() {
            let vv = vector<u8>[];
            vv;
           //^ vector<u8>   
        }
    }        
    """)

    fun `test vector lit with inferred type`() = testExpr("""
    module 0x1::main {
        fun main() {
            let vv = vector[1u8];
            vv;
           //^ vector<u8>   
        }
    }        
    """)

    fun `test vector lit with inferred integer type`() = testExpr("""
    module 0x1::main {
        fun main() {
            let vv = vector[1];
            vv;
           //^ vector<integer>   
        }
    }        
    """)

    fun `test vector lit with inferred type from call expr`() = testExpr("""
    module 0x1::main {
        fun call(a: vector<u8>) {}
        fun main() {
            let vv = vector[];
            call(vv);
            vv;
           //^ vector<u8>   
        }
    }        
    """)

    fun `test vector lit with explicit type and type error`() = testExpr("""
    module 0x1::main {
        fun main() {
            let vv = vector<u8>[1u64];
            vv;
           //^ vector<u8>   
        }
    }        
    """)

    fun `test vector lit with implicit type and type error`() = testExpr("""
    module 0x1::main {
        fun main() {
            let vv = vector[1u8, 1u64];
            vv;
           //^ vector<u8>   
        }
    }        
    """)

    fun `test vector lit inside specs`() = testExpr("""
    module 0x1::main {
        spec module {
            let vv = vector[1];
            vv;
           //^ vector<num>   
        }
    }        
    """)

    fun `test call expr with explicit type and type error`() = testExpr("""
    module 0x1::main {
        fun call<T>(a: T, b: T): T {
            b        
        }    
        fun main() {
            let aa = call<u8>(1u64, 1u128);
            aa;
          //^ u8  
        }    
    }        
    """)

    fun `test call expr with implicit type and type error`() = testExpr("""
    module 0x1::main {
        fun call<T>(a: T, b: T): T {
            b        
        }    
        fun main() {
            let aa = call(1u8, 1u128);
            aa;
          //^ u8  
        }    
    }        
    """)

    fun `test simple map vector field`() = testExpr(
        """
module 0x1::simple_map {
    struct SimpleMap<Value> has copy, drop, store {
        data: vector<Value>,
    }

    /// Create an empty vector.
    native public fun vector_empty<Element>(): vector<Element>;
    
    public fun create<FunValue: store>(): SimpleMap<FunValue> {
        SimpleMap {
            data: vector_empty(),
        }
    }
    
    fun main() {
        let map = create<u64>();
        let map_data = &map.data;
        map_data;
        //^ &vector<u64>
    }
}        
    """
    )

    fun `test recursive type`() = testExpr("""
module 0x1::main {
    struct S { val: S }
    fun main() {
        let s = S { val: };
        s;
      //^ 0x1::main::S  
    }
}        
    """)

    fun `test recursive type with nested struct`() = testExpr("""
module 0x1::main {
    struct S { val: vector<vector<S>> }
    fun main() {
        let s = S { val: };
        s;
      //^ 0x1::main::S  
    }
}        
    """)

    fun `test nested struct literal explicit type`() = testExpr("""
module 0x1::main {
    struct V<T> { val: T }
    struct S<T> { val: V<T> }
    fun main() {
        let s = S { val: V<u64> { val: }};
        s;
      //^ 0x1::main::S<u64>  
    }
}        
    """)

    fun `test nested struct literal inferred type`() = testExpr("""
module 0x1::main {
    struct V<T> { val: T }
    struct S<T> { val: V<T> }
    fun main() {
        let s = S { val: V { val: 1u64 }};
        s;
      //^ 0x1::main::S<u64>  
    }
}        
    """)

    fun `test parens type`() = testExpr("""
module 0x1::main {
    fun call(a: (u8)) {
        a;
      //^ u8  
    }
}        
    """)

    fun `test imported table with alias`() = testExpr(
        """
module 0x1::table_with_length {
    struct TableWithLength<phantom K: copy + drop, phantom V> has store {}
}
module 0x1::pool {
    use 0x1::table_with_length::TableWithLength as Table;
    struct Pool has store {
        shares: Table<address, u128>,
    }

    fun add_shares(pool: &mut Pool) {
        let shares = pool.shares;
        shares;
        //^ 0x1::table_with_length::TableWithLength<address, u128>
    }
}
    """
    )

    fun `test call expr from alias`() = testExpr("""
module 0x1::string {
    public fun call(): u8 {}
}        
module 0x1::main {
    use 0x1::string::call as mycall;
    fun main() {
        let a = mycall();
        a;
      //^ u8  
    }
}
    """)

    fun `test binding inside if block`() = testExpr("""
module 0x1::main {
    fun main() {
        if (true) {
            let in = 1;
            in;
          //^ integer  
        }
    }
}        
    """)

    fun `test binding inside else block`() = testExpr("""
module 0x1::main {
    fun main() {
        if (true) {} else {
            let in = 1;
            in;
          //^ integer  
        }
    }
}        
    """)

    fun `test binding inside code block`() = testExpr("""
module 0x1::main {
    fun main() {
        let b = {
            let in = 1;
            in;
          //^ integer  
        };
    }
}        
    """)

    fun `test bit and`() = testExpr("""
module 0x1::main {
    fun main() {
        (1 & 1);
      //^ integer  
    }
}        
    """)

    fun `test bit or`() = testExpr("""
module 0x1::main {
    fun main() {
        (1 | 1);
      //^ integer  
    }
}        
    """)

    fun `test bit shift left`() = testExpr("""
module 0x1::main {
    fun main() {
        (1 << 1);
      //^ integer  
    }
}        
    """)

    fun `test bit shift right`() = testExpr("""
module 0x1::main {
    fun main() {
        (1 >> 1);
      //^ integer  
    }
}        
    """)

    fun `test bit ^`() = testExpr("""
module 0x1::main {
    fun main() {
        (1 ^ 1);
      //^ integer  
    }
}        
    """)

    fun `test infer result for spec`() = testExpr("""
module 0x1::main {
    struct S { val: u8 }
    fun call(): S { S { val: 1 } }
    spec call {
        ensures result.val == 1;
                      //^ num
    }
}        
    """)

    fun `test struct unpacking type`() = testExpr("""
        module 0x1::m {
            struct S<CoinType> { amount: CoinType }
            fun call<CallCoinType>(s: S<CallCoinType>) {
                let S { amount: my_amount } = s;
                my_amount;
                //^ CallCoinType
            }
        }               
    """)

    fun `test type of binding of tuple of single variable`() = testExpr("""
        module 0x1::m {
            fun call() {
                let (a) = 1;
                a;
              //^ integer  
            }
        }        
    """)

    fun `test type of binding of tuple of single variable with comma`() = testExpr("""
        module 0x1::m {
            fun call() {
                let (a,) = 1;
                a;
              //^ integer  
            }
        }        
    """)

    fun `test deref type with generics`() = testExpr("""
        module 0x1::m {
            struct Coin<CoinType> { val: u8 }
            struct BTC {}
            fun main() {
                let a = &&mut Coin { val: 10 };
                let b: Coin<BTC> = **a;
                a;
              //^ &&mut 0x1::m::Coin<_>  
            }        
        } 
    """)

    fun `test infer integer with explicit tuple literal type in let statement`() = testExpr("""
        module 0x1::m {
            fun call<Element>(v: Element): Element {}
            fun main() {
                let u = 1;
                let (a, b): (u8, u8) = (call(u), call(u));
                u;
              //^ u8  
            }        
        } 
    """)

    fun `test infer integer with explicit tuple literal type in assignment`() = testExpr("""
        module 0x1::m {
            fun call<Element>(v: Element): Element {}
            fun main() {
                let u = 1;
                let (a, b): (u8, u8);
                (a, b) = (call(u), call(u));
                u;
              //^ u8  
            }        
        } 
    """)

    fun `test integer parameter has type num in inline spec block`() = testExpr("""
        module 0x1::m {
            fun main(degree: u8) {
                spec {
                    degree;
                    //^ num
                }
            }
        }        
    """)

    fun `test continue expr never type`() = testExpr("""
        module 0x1::m {
            fun main() {
                while (true) {
                    continue 
                    //^ <never>
                }
            }
        }        
    """)

    fun `test break expr never type`() = testExpr("""
        module 0x1::m {
            fun main() {
                while (true) {
                    break  
                    //^ <never>
                }
            }
        }        
    """)

    fun `test abort expr never type`() = testExpr("""
        module 0x1::m {
            fun main() {
                abort 1  
                //^ <never>
            }
        }        
    """)

    fun `test modifies expr`() = testExpr("""
        module 0x1::m {
            struct Coin has key {}
            spec module {
                modifies (global<Coin>(@0x1));
                       //^ 0x1::m::Coin
            }
        }        
    """)

    fun `test builtin const in spec`() = testExpr("""
        module 0x1::m {
            spec module {
                assert 1 <= MAX_U128;
                               //^ num
            }
        }        
    """)

    fun `test num type`() = testExpr("""
        module 0x1::m {
            spec module {
                let a: num;
                a;
              //^ num  
            }
        }        
    """)

    fun `test result of return type of function spec`() = testExpr("""
        module 0x1::m {
            public fun get_fees_distribution(): u128 {
                1
            }
            spec get_fees_distribution {
                aborts_if false;
                ensures result == 1;
                         //^ num
            }
        }
    """)

    fun `test result_1 of return tuple in function spec`() = testExpr("""
        module 0x1::m {
            public fun get_fees_distribution(): (u128, u128) {
                (1, 1)
            }
            spec get_fees_distribution {
                aborts_if false;
                ensures result_1 == 1;
                         //^ num
            }
        }
    """)

    fun `test result_2 of return tuple in function spec`() = testExpr("""
        module 0x1::m {
            public fun get_fees_distribution(): (u128, bool) {
                (1, false)
            }
            spec get_fees_distribution {
                aborts_if false;
                ensures result_2 == 1;
                         //^ bool
            }
        }
    """)

    fun `test spec index expr`() = testExpr("""
        module 0x1::m {
            spec module {
                let v = vector<bool>[false];
                let a = v[1];
                a;
              //^ bool  
            }
        }     
    """)

    fun `test range expr`() = testExpr("""
        module 0x1::m {
            spec module {
                let a = 1..10;
                a;
              //^ range   
            }
        }        
    """)

    fun `test forall quantifier expr`() = testExpr("""
        module 0x1::m {
            fun call() {}
            spec call {
                let a = forall i in 0..10: i < 20;
                a;
              //^ bool  
            }
        }        
    """)

    fun `test forall quantifier range expr`() = testExpr("""
        module 0x1::m {
            fun call() {}
            spec call {
                forall i in 0..10: i < 20;
                            //^ range
            }
        }        
    """)

    fun `test forall quantifier range binding expr`() = testExpr("""
        module 0x1::m {
            fun call() {}
            spec call {
                forall i in 0..10: i < 20;
                                 //^ num
            }
        }        
    """)

    fun `test forall quantifier range vector binding expr`() = testExpr("""
        module 0x1::m {
            fun call() {}
            spec call {
                forall i in vector[true, false]: i;
                                               //^ bool
            }
        }        
    """)

    fun `test forall quantifier type binding expr`() = testExpr("""
        module 0x1::m {
            fun call() {}
            spec call {
                forall i: num : i < 20;
                              //^ num
            }
        }        
    """)

    fun `test exists quantifier expr`() = testExpr("""
        module 0x1::m {
            fun call() {}
            spec call {
                let a = exists i in 0..10: i == 1;
                a;
              //^ bool  
            }
        }        
    """)

    fun `test spec expr with visibility inferred`() = testExpr("""
        module 0x1::m {
            spec module {
                let i = false;
                invariant [suspendable] i;
                                      //^ bool
            }
        }        
    """)

    fun `test spec vector slice`() = testExpr("""
        module 0x1::m {
            spec module {
                let v = vector[true, false];
                let slice = v[0..1];
                slice;
                //^ vector<bool>
            }
        }        
    """)

    fun `test integer type of shift left`() = testExpr("""
        module 0x1::m {
            fun main() {
                let a = 1u8;
                (a << 32);
              //^ u8  
            }
        }        
    """)

    fun `test integer type of shift right`() = testExpr("""
        module 0x1::m {
            fun main() {
                let a = 1u8;
                (a >> 32);
              //^ u8  
            }
        }        
    """)

    fun `test integer type inference with ordering expr`() = testExpr("""
        module 0x1::m {
            fun main() {
                let a = 1;
                a > 2u8;
                a;
              //^ u8  
            }
        }        
    """)

    fun `test integer type inference with equality expr`() = testExpr("""
        module 0x1::m {
            fun main() {
                let a = 1;
                a == 2u8;
                a;
              //^ u8  
            }
        }        
    """)
}
