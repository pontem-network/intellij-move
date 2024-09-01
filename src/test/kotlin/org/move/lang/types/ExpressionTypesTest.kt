package org.move.lang.types

import org.move.utils.tests.NamedAddress
import org.move.utils.tests.types.TypificationTestCase

class ExpressionTypesTest : TypificationTestCase() {
    fun `test add expr`() = testExpr(
        """
    script {
        fun main() {
            (1u8 + 1u8);
          //^ u8
        }
    }    
    """
    )

    fun `test sub expr`() = testExpr(
        """
    script {
        fun main() {
            (1u8 - 1u8);
          //^ u8
        }
    }    
    """
    )

    fun `test mul expr`() = testExpr(
        """
    script {
        fun main() {
            (1u8 * 1u8);
          //^ u8
        }
    }    
    """
    )

    fun `test div expr`() = testExpr(
        """
    script {
        fun main() {
            (1u8 / 1u8);
          //^ u8
        }
    }    
    """
    )

    fun `test mod expr`() = testExpr(
        """
    script {
        fun main() {
            (1u8 % 10);
          //^ u8
        }
    }    
    """
    )

    fun `test bang expr`() = testExpr(
        """
    script {
        fun main() {
            !true;
          //^ bool
        }
    }    
    """
    )

    fun `test less expr`() = testExpr(
        """
    script {
        fun main() {
            (1 < 1);
          //^ bool
        }
    }    
    """
    )

    fun `test less equal expr`() = testExpr(
        """
    script {
        fun main() {
            (1 <= 1);
          //^ bool
        }
    }    
    """
    )

    fun `test greater expr`() = testExpr(
        """
    script {
        fun main() {
            (1 > 1);
          //^ bool
        }
    }    
    """
    )

    fun `test greater equal expr`() = testExpr(
        """
    script {
        fun main() {
            (1 >= 1);
          //^ bool
        }
    }    
    """
    )

    fun `test cast expr`() = testExpr(
        """
    script {
        fun main() {
            (1 as u8);
          //^ u8
        }
    }    
    """
    )

    fun `test copy expr`() = testExpr(
        """
    script {
        fun main() {
            copy 1u8;
          //^ u8
        }
    }    
    """
    )

    fun `test move expr`() = testExpr(
        """
    script {
        fun main() {
            move 1u8;
          //^ u8
        }
    }    
    """
    )

    fun `test struct literal expr with unresolved type param`() = testExpr(
        """
    module 0x1::M {
        struct R<CoinType> {}
        fun main() {
            R {};
          //^ 0x1::M::R<<unknown>>  
        }
    }
    """
    )

    fun `test borrow expr`() = testExpr(
        """
    module 0x1::M {
        fun main(s: signer) {
            &s;
          //^ &signer 
        }
    }    
    """
    )

    fun `test mutable borrow expr`() = testExpr(
        """
    module 0x1::M {
        fun main(s: signer) {
            &mut s;
          //^ &mut signer 
        }
    }    
    """
    )

    fun `test deref expr`() = testExpr(
        """
    module 0x1::M {
        fun main(s: &signer) {
            *s;
          //^ signer 
        }
    }    
    """
    )

    fun `test dot access to primitive field`() = testExpr(
        """
    module 0x1::M {
        struct S { addr: address }
        fun main() {
            let s = S { addr: @0x1 };
            ((&s).addr);
          //^ address 
        }
    }    
    """
    )

    fun `test dot access to field with struct type`() = testExpr(
        """
    module 0x1::M {
        struct Addr {}
        struct S { addr: Addr }
        fun main() {
            let s = S { addr: Addr {} };
            ((&s).addr);
          //^ 0x1::M::Addr 
        }
    }    
    """
    )

    fun `test borrow expr of dot access`() = testExpr(
        """
    module 0x1::M {
        struct Addr {}
        struct S { addr: Addr }
        fun main() {
            let s = S { addr: Addr {} };
            &mut s.addr;
          //^ &mut 0x1::M::Addr 
        }
    }    
    """
    )

    fun `test add expr with untyped and typed integer`() = testExpr(
        """
    module 0x1::M {
        fun main() {
            (1 + 1u8);
          //^ u8  
        }
    }    
    """
    )

    fun `test add expr with untyped and typed integer reversed`() = testExpr(
        """
    module 0x1::M {
        fun main() {
            (1u8 + 1);
          //^ u8  
        }
    }    
    """
    )

    fun `test struct field as vector`() = testExpr(
        """
    module 0x1::M {
        struct NFT {}
        struct Collection { nfts: vector<NFT> }
        fun m(coll: Collection) {
            (coll.nfts);
          //^ vector<0x1::M::NFT>  
        }
    }    
    """
    )

    fun `test if expr`() = testExpr(
        """
    module 0x1::M {
        fun m() {
            let a = if (true) 1 else 2;
            a;
          //^ integer 
        }
    }    
    """
    )

    fun `test if expr without else`() = testExpr(
        """
    module 0x1::M {
        fun m() {
            let a = if (true) 1;
            a;
          //^ () 
        }
    }    
    """
    )

    fun `test if expr with incompatible else`() = testExpr(
        """
    module 0x1::M {
        fun m() {
            let a = if (true) 1 else true;
            a;
          //^ <unknown>
        }
    }    
    """
    )

    fun `test return type of unit returning function`() = testExpr(
        """
    module 0x1::M {
        fun call(): () {}
        fun m() {
            call();
          //^ ()
        }
    }    
    """
    )

    fun `test if else with references coerced to less specific one`() = testExpr(
        """
    module 0x1::M {
        struct S {}
        fun m(s: &S, s_mut: &mut S) {
            let cond = true;
            (if (cond) s_mut else s);
          //^ &0x1::M::S  
        }
    }    
    """
    )

    fun `test x string`() = testExpr(
        """
    module 0x1::M {
        fun m() {
            x"1234";
            //^ vector<u8>
        }
    }    
    """
    )

    fun `test msl num`() = testExpr(
        """
    module 0x1::M {
        spec module {
            1;
          //^ num  
        }
    }    
    """
    )

    fun `test msl callable num`() = testExpr(
        """
    module 0x1::M {
        fun call(): u8 { 1 }
        spec module {
            call();
            //^ num
        }
    }    
    """
    )

    fun `test msl ref is type`() = testExpr(
        """
    module 0x1::M {
        struct S {}
        fun ref(): &S { &S {} }
        spec module {
            let a = ref();
            a;
          //^ 0x1::M::S
        }
    }    
    """
    )

    fun `test msl mut ref is type`() = testExpr(
        """
    module 0x1::M {
        struct S {}
        fun ref_mut(): &mut S {}
        spec module {
            let a = ref_mut();
            a;
          //^ 0x1::M::S
        }
    }    
    """
    )

    fun `test type of fun param in spec`() = testExpr(
        """
    module 0x1::M {
        fun call(addr: address) {}
        spec call {
            addr;
            //^ address
        }
    }    
    """
    )

    fun `test type of u8 fun param in spec`() = testExpr(
        """
    module 0x1::M {
        fun call(n: u8) {}
        spec call {
            n;
          //^ num  
        }
    }    
    """
    )

    fun `test old function type for spec`() = testExpr(
        """
    module 0x1::M {
        struct S {}
        fun call(a: S) {}
        spec call {
            old(a);
          //^ 0x1::M::S 
        }
    }    
    """
    )

    fun `test global function type for spec`() = testExpr(
        """
    module 0x1::M {
        struct S has key {}
        spec module {
            let a = global<S>(@0x1);
            a;
          //^ 0x1::M::S 
        }
    }    
    """
    )

    fun `test const int in spec`() = testExpr(
        """
    module 0x1::M {
        const MY_INT: u8 = 1;
        spec module {
            MY_INT;
            //^ num
        }
    }    
    """
    )

    fun `test schema field type`() = testExpr(
        """
    module 0x1::M {
        spec schema SS {
            val: num;
            val;
            //^ num
        }
    }    
    """
    )

    fun `test struct field vector_u8 in spec`() = testExpr(
        """
    module 0x1::M {
        struct S { vec: vector<u8> } 
        spec module {
            let s = S { vec: b"" };
            s.vec
            //^ vector<num>
        }
    }    
    """
    )

    fun `test tuple type`() = testExpr(
        """
    module 0x1::M {
        fun m() {
            (1u64, 1u64);
          //^ (u64, u64)  
        }
    }    
    """
    )

    fun `test explicit generic type struct`() = testExpr(
        """
    module 0x1::M {
        struct Option<Element> {}
        fun call() {
            let a = Option<u8> {};
            a;
          //^ 0x1::M::Option<u8>  
        }
    }        
    """
    )

    fun `test type of plus with invalid arguments`() = testExpr(
        """
    module 0x1::M {
        fun add(a: bool, b: bool) {
            (a + b);
          //^ <unknown>  
        }
    }    
    """
    )

    fun `test struct lit with generic of type with incorrect abilities`() = testExpr(
        """
    module 0x1::M {
        struct S<phantom Message: store> {}
        struct R has copy {  }
        fun main() {
            S<R> {};
          //^ 0x1::M::S<0x1::M::R>  
        }
    }    
    """
    )

    fun `test while expr returns unit`() = testExpr(
        """
    module 0x1::M {
        fun main() {
            let a = while (true) { 1; };
            a;
          //^ <never>  
        }
    }    
    """
    )

    fun `test return value from block`() = testExpr(
        """
    module 0x1::M {
        fun main() {
            let a = { 1u8 };
            a;
          //^ u8  
        }
    }    
    """
    )

    fun `test if else return`() = testExpr(
        """
    module 0x1::M {
        fun main() {
            if (true) { return 1 } else { return 2 };
          //^ <never>  
        }
    }    
    """
    )

    fun `test unpack struct into field`() = testExpr(
        """
        module 0x1::M {
        struct S { val: u8 }
        fun s(): S { S { val: 10 } }
        fun main() {
            let s = s();
            s;
          //^ 0x1::M::S   
        }
    }            
    """
    )

    fun `test unpack tuple of structs`() = testExpr(
        """
        module 0x1::M {
        struct S { val: u8 }
        fun s(): (S, S) { (S { val: 10 }, S { val: 10 }) }
        fun main() {
            let (s, t) = s();
            s;
          //^ 0x1::M::S   
        }
    }            
    """
    )

    fun `test integer inference with spec blocks inside block`() = testExpr(
        """
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
    """
    )

    fun `test integer inference with spec blocks outside block`() = testExpr(
        """
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
    """
    )

    fun `test vector lit with explicit type`() = testExpr(
        """
    module 0x1::main {
        fun main() {
            let vv = vector<u8>[];
            vv;
           //^ vector<u8>   
        }
    }        
    """
    )

    fun `test vector lit with inferred type`() = testExpr(
        """
    module 0x1::main {
        fun main() {
            let vv = vector[1u8];
            vv;
           //^ vector<u8>   
        }
    }        
    """
    )

    fun `test vector lit with inferred integer type`() = testExpr(
        """
    module 0x1::main {
        fun main() {
            let vv = vector[1];
            vv;
           //^ vector<integer>   
        }
    }        
    """
    )

    fun `test vector lit with inferred type from call expr`() = testExpr(
        """
    module 0x1::main {
        fun call(a: vector<u8>) {}
        fun main() {
            let vv = vector[];
            call(vv);
            vv;
           //^ vector<u8>   
        }
    }        
    """
    )

    fun `test vector lit with explicit type and type error`() = testExpr(
        """
    module 0x1::main {
        fun main() {
            let vv = vector<u8>[1u64];
            vv;
           //^ vector<u8>   
        }
    }        
    """
    )

    fun `test vector lit with implicit type and type error`() = testExpr(
        """
    module 0x1::main {
        fun main() {
            let vv = vector[1u8, 1u64];
            vv;
           //^ vector<u8>   
        }
    }        
    """
    )

    fun `test vector lit inside specs`() = testExpr(
        """
    module 0x1::main {
        spec module {
            let vv = vector[1];
            vv;
           //^ vector<num>   
        }
    }        
    """
    )

    fun `test call expr with explicit type and type error`() = testExpr(
        """
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
    """
    )

    fun `test call expr with implicit type and type error`() = testExpr(
        """
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
    """
    )

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

    fun `test recursive type`() = testExpr(
        """
module 0x1::main {
    struct S { val: S }
    fun main() {
        let s = S { val: };
        s;
      //^ 0x1::main::S  
    }
}        
    """
    )

    fun `test recursive type with nested struct`() = testExpr(
        """
module 0x1::main {
    struct S { val: vector<vector<S>> }
    fun main() {
        let s = S { val: };
        s;
      //^ 0x1::main::S  
    }
}        
    """
    )

    fun `test nested struct literal explicit type`() = testExpr(
        """
module 0x1::main {
    struct V<T> { val: T }
    struct S<T> { val: V<T> }
    fun main() {
        let s = S { val: V<u64> { val: }};
        s;
      //^ 0x1::main::S<u64>  
    }
}        
    """
    )

    fun `test nested struct literal inferred type`() = testExpr(
        """
module 0x1::main {
    struct V<T> { val: T }
    struct S<T> { val: V<T> }
    fun main() {
        let s = S { val: V { val: 1u64 }};
        s;
      //^ 0x1::main::S<u64>  
    }
}        
    """
    )

    fun `test parens type`() = testExpr(
        """
module 0x1::main {
    fun call(a: (u8)) {
        a;
      //^ u8  
    }
}        
    """
    )

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

    fun `test call expr from alias`() = testExpr(
        """
module 0x1::string {
    public fun call(): u8 { 1 }
}        
module 0x1::main {
    use 0x1::string::call as mycall;
    fun main() {
        let a = mycall();
        a;
      //^ u8  
    }
}
    """
    )

    fun `test binding inside if block`() = testExpr(
        """
module 0x1::main {
    fun main() {
        if (true) {
            let in = 1;
            in;
          //^ integer  
        }
    }
}        
    """
    )

    fun `test binding inside else block`() = testExpr(
        """
module 0x1::main {
    fun main() {
        if (true) {} else {
            let in = 1;
            in;
          //^ integer  
        }
    }
}        
    """
    )

    fun `test binding inside code block`() = testExpr(
        """
module 0x1::main {
    fun main() {
        let b = {
            let in = 1;
            in;
          //^ integer  
        };
    }
}        
    """
    )

    fun `test bit and`() = testExpr(
        """
module 0x1::main {
    fun main() {
        (1 & 1);
      //^ integer  
    }
}        
    """
    )

    fun `test bit or`() = testExpr(
        """
module 0x1::main {
    fun main() {
        (1 | 1);
      //^ integer  
    }
}        
    """
    )

    fun `test bit shift left`() = testExpr(
        """
module 0x1::main {
    fun main() {
        (1 << 1);
      //^ integer  
    }
}        
    """
    )

    fun `test bit shift right`() = testExpr(
        """
module 0x1::main {
    fun main() {
        (1 >> 1);
      //^ integer  
    }
}        
    """
    )

    fun `test bit ^`() = testExpr(
        """
module 0x1::main {
    fun main() {
        (1 ^ 1);
      //^ integer  
    }
}        
    """
    )

    fun `test infer result for spec`() = testExpr(
        """
module 0x1::main {
    struct S { val: u8 }
    fun call(): S { S { val: 1 } }
    spec call {
        ensures result.val == 1;
                      //^ num
    }
}        
    """
    )

    fun `test struct unpacking type`() = testExpr(
        """
        module 0x1::m {
            struct S<CoinType> { amount: CoinType }
            fun call<CallCoinType>(s: S<CallCoinType>) {
                let S { amount: my_amount } = s;
                my_amount;
                //^ CallCoinType
            }
        }               
    """
    )

    fun `test type of binding of tuple of single variable`() = testExpr(
        """
        module 0x1::m {
            fun call() {
                let (a) = 1;
                a;
              //^ integer  
            }
        }        
    """
    )

    fun `test type of binding of tuple of single variable with comma`() = testExpr(
        """
        module 0x1::m {
            fun call() {
                let (a,) = 1;
                a;
              //^ integer  
            }
        }        
    """
    )

    fun `test deref type with generics`() = testExpr(
        """
        module 0x1::m {
            struct Coin<CoinType> { val: u8 }
            struct BTC {}
            fun main() {
                let a = &mut Coin { val: 10 };
                let b: Coin<BTC> = *a;
                a;
              //^ &mut 0x1::m::Coin<0x1::m::BTC>  
            }        
        } 
    """
    )

    fun `test infer integer with explicit tuple literal type in let statement`() = testExpr(
        """
        module 0x1::m {
            fun call<Element>(v: Element): Element {}
            fun main() {
                let u = 1;
                let (a, b): (u8, u8) = (call(u), call(u));
                u;
              //^ u8  
            }        
        } 
    """
    )

    fun `test infer integer with explicit tuple literal type in assignment`() = testExpr(
        """
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
    """
    )

    fun `test integer parameter has type num in inline spec block`() = testExpr(
        """
        module 0x1::m {
            fun main(degree: u8) {
                spec {
                    degree;
                    //^ num
                }
            }
        }        
    """
    )

    fun `test continue expr never type`() = testExpr(
        """
        module 0x1::m {
            fun main() {
                while (true) {
                    continue 
                    //^ <never>
                }
            }
        }        
    """
    )

    fun `test break expr never type`() = testExpr(
        """
        module 0x1::m {
            fun main() {
                while (true) {
                    break  
                    //^ <never>
                }
            }
        }        
    """
    )

    fun `test abort expr never type`() = testExpr(
        """
        module 0x1::m {
            fun main() {
                abort 1  
                //^ <never>
            }
        }        
    """
    )

    fun `test modifies expr`() = testExpr(
        """
        module 0x1::m {
            struct Coin has key {}
            spec module {
                modifies (global<Coin>(@0x1));
                       //^ 0x1::m::Coin
            }
        }        
    """
    )

    fun `test builtin const in spec`() = testExpr(
        """
        module 0x1::m {
            spec module {
                assert 1 <= MAX_U128;
                               //^ num
            }
        }        
    """
    )

    fun `test num type`() = testExpr(
        """
        module 0x1::m {
            spec module {
                let a: num;
                a;
              //^ num  
            }
        }        
    """
    )

    fun `test result of return type of function spec`() = testExpr(
        """
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
    """
    )

    fun `test result_1 of return tuple in function spec`() = testExpr(
        """
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
    """
    )

    fun `test result_2 of return tuple in function spec`() = testExpr(
        """
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
    """
    )

    fun `test range expr`() = testExpr(
        """
        module 0x1::m {
            spec module {
                let a = 1..10;
                a;
              //^ range<num>   
            }
        }        
    """
    )

    fun `test forall quantifier expr`() = testExpr(
        """
        module 0x1::m {
            fun call() {}
            spec call {
                let a = forall i in 0..10: i < 20;
                a;
              //^ bool  
            }
        }        
    """
    )

    fun `test forall quantifier range expr`() = testExpr(
        """
        module 0x1::m {
            fun call() {}
            spec call {
                forall i in 0..10: i < 20;
                            //^ range<num>
            }
        }        
    """
    )

    fun `test forall quantifier range binding expr`() = testExpr(
        """
        module 0x1::m {
            fun call() {}
            spec call {
                forall i in 0..10: i < 20;
                                 //^ num
            }
        }        
    """
    )

    fun `test forall quantifier range vector binding expr`() = testExpr(
        """
        module 0x1::m {
            fun call() {}
            spec call {
                forall i in vector[true, false]: i;
                                               //^ bool
            }
        }        
    """
    )

    fun `test forall quantifier type binding expr`() = testExpr(
        """
        module 0x1::m {
            fun call() {}
            spec call {
                forall i: num : i < 20;
                              //^ num
            }
        }        
    """
    )

    fun `test exists quantifier expr`() = testExpr(
        """
        module 0x1::m {
            fun call() {}
            spec call {
                let a = exists i in 0..10: i == 1;
                a;
              //^ bool  
            }
        }        
    """
    )

    fun `test spec expr with visibility inferred`() = testExpr(
        """
        module 0x1::m {
            spec module {
                let i = false;
                invariant [suspendable] i;
                                      //^ bool
            }
        }        
    """
    )

    fun `test spec vector slice`() = testExpr(
        """
        module 0x1::m {
            spec module {
                let v = vector[true, false];
                let slice = v[0..1];
                slice;
                //^ vector<bool>
            }
        }        
    """
    )

    fun `test integer type of shift left`() = testExpr(
        """
        module 0x1::m {
            fun main() {
                let a = 1u8;
                (a << 32);
              //^ u8  
            }
        }        
    """
    )

    fun `test integer type of shift right`() = testExpr(
        """
        module 0x1::m {
            fun main() {
                let a = 1u8;
                (a >> 32);
              //^ u8  
            }
        }        
    """
    )

    fun `test integer type inference with ordering expr`() = testExpr(
        """
        module 0x1::m {
            fun main() {
                let a = 1;
                a > 2u8;
                a;
              //^ u8  
            }
        }        
    """
    )

    fun `test integer type inference with equality expr`() = testExpr(
        """
        module 0x1::m {
            fun main() {
                let a = 1;
                a == 2u8;
                a;
              //^ u8  
            }
        }        
    """
    )

    fun `test lambda expr`() = testExpr(
        """
        module 0x1::m {
            inline fun main<Element>(f: |Element|) {
                f;
              //^ |Element| -> ()  
            }
        }        
    """
    )

    fun `test lambda expr from assigned variable`() = testExpr(
        """
        module 0x1::m {
            inline fun main<Element>(f: |Element|) {
                let g = f;
                g;
              //^ |Element| -> ()  
            }
        }        
    """
    )

    fun `test lambda expr unit type`() = testExpr(
        """
        module 0x1::m {
            inline fun main<Element>(f: |Element|) {
                f();
              //^ ()  
            }
        }        
    """
    )

    fun `test lambda expr returning type`() = testExpr(
        """
        module 0x1::m {
            inline fun main<Element>(f: |Element| Element) {
                f();
              //^ Element  
            }
        }        
    """
    )

    fun `test lambda expr integer return type`() = testExpr(
        """
        module 0x1::m {
            inline fun main<Element>(e: Element, f: |Element| u8) {
                f(e);
              //^ u8  
            }
        }        
    """
    )

    fun `test infer let binding pat type in spec block arbitrary order`() = testExpr(
        """
        module 0x1::m {
            spec module {
                addr;
                //^ address
                let addr = @0x1;
            }
        }        
    """
    )

    fun `test infer let post binding pat type in spec block arbitrary order`() = testExpr(
        """
        module 0x1::m {
            spec module {
                addr;
                //^ address
                let post addr = @0x1;
            }
        }        
    """
    )

    fun `test int2bv and bv type`() = testExpr(
        """
        module 0x1::m {
            fun call() {}
            spec call {
                let a = int2bv(100);
                a;
              //^ bv  
            }
        }        
    """
    )

    fun `test int2bv bv2int`() = testExpr(
        """
        module 0x1::m {
            fun call() {}
            spec call {
                let a = bv2int(int2bv(100));
                a;
              //^ num  
            }
        }        
    """
    )

    fun `test infer no return lambda expr`() = testExpr(
        """
        module 0x1::m {
            inline fun for_each<Element>(v: vector<Element>, f: |Element|) {}
            fun main() {
                for_each(vector[1, 2, 3], |elem| { elem; });
                                                  //^ integer
            }
        }
    """
    )

    fun `test infer identity lambda expr`() = testExpr(
        """
        module 0x1::m {
            inline fun for_each<Element>(v: vector<Element>, f: |Element| Element) {}
            fun main() {
                for_each(vector[1, 2, 3], |elem| elem);
                                                  //^ integer
            }
        }
    """
    )

    fun `test infer two param lambda expr`() = testExpr(
        """
        module 0x1::m {
            inline fun for_each<Element>(v: vector<Element>, f: |Element, Element|) {}
            fun main() {
                for_each(vector[1, 2, 3], |elem1, elem2| { elem2; });
                                                          //^ integer
            }
        }
    """
    )

    fun `test infer single param lambda two param expected`() = testExpr(
        """
        module 0x1::m {
            inline fun for_each<Element>(v: vector<Element>, f: |Element, Element|) {}
            fun main() {
                for_each(vector[1, 2, 3], |elem| { elem; });
                                                    //^ integer
            }
        }
    """
    )

    fun `test infer vector type binding of spec fun`() = testExpr(
        """
        module 0x1::m {
            spec module {
                fun eq_push_back<Element>(v1: vector<Element>, v2: vector<Element>, e: Element): bool {
                    let res = 
                    (len(v1) == len(v2) + 1 &&
                        v1[len(v1)-1] == e &&
                        v1[0..len(v1)-1] == v2[0..len(v2)]);
                    res
                    //^ bool
                }
            }
        }        
    """
    )

    fun `test item spec struct field`() = testExpr(
        """
        module 0x1::m {
            struct Option<Element> has copy, drop, store {
                vec: vector<Element>
            }
            struct S { aggregator: Option<u8> }
            spec S {
                aggregator;
                //^ 0x1::m::Option<num>
            }
        }        
    """
    )

    fun `test generic result type in function item spec should not crash`() = testExprsTypified(
        """
        module 0x1::m {
            native struct TableWithLength<phantom K: copy + drop, phantom V> has store;
            struct BigVector<T> has store {
                buckets: TableWithLength<u64, vector<T>>,
                bucket_size: u64
            }
            spec native fun table_with_length_spec_get<K, V>(t: TableWithLength<K, V>, k: K): V;
            native fun vector_borrow<Element>(v: &vector<Element>, i: u64): &Element;

            native fun borrow<T>(v: &BigVector<T>, i: u64): &T;

            spec borrow {
                ensures result == vector_borrow(
                    table_with_length_spec_get(v.buckets, i / v.bucket_size), i % v.bucket_size
                );
            }
        }
    """
    )

    fun `test option of object equals view function with the same name as module`() = testExprsTypified(
        """
        module 0x1::royalty {
            struct Royalty has copy, drop, key {
                numerator: u64,
                denominator: u64,
                payee_address: address,
            }
            public native fun create(): Royalty;
        }
        module 0x1::m {
            use 0x1::royalty::{Self, Royalty};
            struct Option<Element> has copy, drop, store {
                vec: vector<Element>
            }
            public native fun some<SomeElement>(e: SomeElement): Option<SomeElement>;
            public native fun royalty<T: key>(): Option<Royalty>;
            #[test]
            fun test_some() {
                let expected_royalty = royalty::create(); 
                assert!(some(expected_royalty) == royalty());
            }
        }        
    """
    )

    fun `test ref expr of struct item`() = testExpr(
        """
        module 0x1::m {
            struct S {}
            fun main() {
                S;
              //^ <unknown>  
            }
        }        
    """
    )

    fun `test ref expr of schema item`() = testExpr(
        """
        module 0x1::m {
            spec schema S {}
            fun main() {
                S;
              //^ <unknown>  
            }
        }        
    """
    )

    fun `test ref expr of function item`() = testExpr(
        """
        module 0x1::m {
            fun call() {}
            fun main() {
                call;
              //^ <unknown>  
            }
        }        
    """
    )

    fun `test incomplete range expr`() = testExpr("""
        module 0x1::m {
            spec module {
                let a = 1..;
                a;
              //^ range<num>  
            }
        }        
    """)

    fun `test dot expr chained`() = testExpr("""
        module 0x1::m {
            struct Pool { field: u8 }
            fun main(pool: &mut Pool) {
                pool.
                //^ &mut 0x1::m::Pool
                pool.field;
            }
        }        
    """)

    fun `test dot expr with dot expr incomplete 1`() = testExpr("""
        module 0x1::m {
            struct Pool { field: u8 }
            fun main(pool: &mut Pool) {
                pool.field
                //^ &mut 0x1::m::Pool
                pool.field
            }
        }        
    """)

    fun `test dot expr with dot expr incomplete 2`() = testExpr("""
        module 0x1::m {
            struct Pool { field: u8 }
            fun main(pool: &mut Pool) {
                pool.unknown
                pool.field
                    //^ u8
            }
        }        
    """)

    fun `test global variable for schema`() = testExpr("""
        module 0x1::m {
            spec module {
                global supply: num;
            }
            spec schema MySchema {
                ensures supply == 1;
                          //^ num   
            }
        }        
    """)

    fun `test no error with type inference with pragma`() = testExpr("""
        module 0x1::m {
            struct Table { length: u64 }
        }        
        spec 0x1::m {
            spec Table {
                pragma map_length = length;
                                    //^ num
            }
        }
    """)

    fun `test infer quant variable in forall`() = testBinding("""
        module 0x1::m {}        
        spec 0x1::m {
            spec Table {
                let left_length = 100;
                let left = vector[];
                let right = vector[];
                ensures forall i: u64 where i < left_length: left[i] == right[i];
                             //^ num
            }
        }
    """)

    fun `test infer quant variable in exists`() = testBinding("""
        module 0x1::m {}        
        spec 0x1::m {
            spec Table {
                let left_length = 100;
                let left = vector[];
                let right = vector[];
                ensures exists i: u64 where i < left_length: left[i] == right[i];
                             //^ num
            }
        }
    """)

    fun `test infer quant variable in where`() = testExpr("""
        module 0x1::m {}        
        spec 0x1::m {
            spec Table {
                let left_length = 100;
                let left = vector[];
                let right = vector[];
                ensures forall i: u64 where i < left_length: left[i] == right[i];
                                          //^ num
            }
        }
    """)

    fun `test infer quant variable in quant body`() = testExpr("""
        module 0x1::m {}        
        spec 0x1::m {
            spec Table {
                let left_length = 100;
                let left = vector[];
                let right = vector[];
                ensures forall i: u64 where i < left_length: left[i] == right[i];
                                                                //^ num
            }
        }
    """)

    fun `test for expr index partial`() = testExpr("""
        module 0x1::m {
            fun main() {
                for (i in ) {
                    i;
                  //^ <unknown>  
                };
            }
        }        
    """)

    fun `test for expr index range expr int type`() = testExpr("""
        module 0x1::m {
            fun main() {
                for (i in 1..10) {
                    i;
                  //^ integer  
                };
            }
        }        
    """)

    fun `test for expr index range expr bool type`() = testExpr("""
        module 0x1::m {
            fun main() {
                for (i in false..true) {
                    i;
                  //^ bool  
                };
            }
        }        
    """)

    fun `test for expr index with range as variable`() = testExpr("""
        module 0x1::m {
            fun main() {
                let vec = 1..10;
                for (i in vec) {
                    i;
                  //^ integer  
                }
            }
        }        
    """)

    fun `test range as variable`() = testExpr("""
        module 0x1::m {
            fun main() {
                let vec = 1..10;
                vec;
                //^ range<integer>
            }
        }        
    """)

    fun `test unpack struct reference`() = testExpr("""
        module 0x1::m {
            struct Field { id: u8 }
            fun main() {
                let Field { id } = &Field { id: 1 };
                id;
               //^ &u8 
            }
        }                
    """)

    fun `test unpack struct mut reference`() = testExpr("""
        module 0x1::m {
            struct Field { id: u8 }
            fun main() {
                let Field { id } = &mut Field { id: 1 };
                id;
               //^ &mut u8 
            }
        }                
    """)

    fun `test unknown does not influence integer type for function for lhs`() = testExpr(
        """
        module 0x1::option {
            fun some<Element>(e: Element): Element { e }
            fun main() {
                let unknown/*: unknown*/ = unknown_variable;
                let a2 = 1;
                some(a2) == unknown;
                //^ integer
            }
        }        
    """)

    fun `test unknown does not influence integer type for function for rhs`() = testExpr(
        """
        module 0x1::option {
            fun some<Element>(e: Element): Element { e }
            fun main() {
                let unknown/*: unknown*/ = unknown_variable;
                let a2 = 1;
                unknown == some(a2);
                           //^ integer
            }
        }        
    """)

    fun `test no error for eq exprs when combining unknown type items`() = testExpr(
        allowErrors = false,
        code = """
        module 0x1::option {
            struct Option<Element> has copy, drop, store {
                vec: vector<Element>
            }
            fun some<Element>(e: Element): Option<Element> { Option { vec: vector[e] } }
            fun main() {
                let unknown/*: unknown*/ = unknown_variable;
                let a2 = @0x1;
                unknown != some(a2);
                unknown == some(a2);
                           //^ 0x1::option::Option<address>
            }
        }        
    """)

    fun `test infer match expr`() = testExpr("""
        module 0x1::m {
            enum S { One, Two }
            fun main(s: S) {
                match (s) {
                     //^ 0x1::m::S 
                    One => true,
                }
            }
        }        
    """)

    fun `test infer match expr variant lhs`() = testBinding("""
        module 0x1::m {
            enum S { One, Two }
            fun main(s: S) {
                match (s) {
                    One => {},
                   //^ 0x1::m::S
                }
            }
        }        
    """)

    fun `test infer match expr variant rhs`() = testExpr("""
        module 0x1::m {
            enum S { One, Two }
            fun main(s: S) {
                match (s) {
                    S::One => s
                            //^ 0x1::m::S
                }
            }
        }        
    """)

    fun `test infer match expr variant field`() = testExpr("""
        module 0x1::m {
            enum S { One { field: u8 }, Two { field: u8 } }
            fun main(s: S) {
                match (s) {
                    One => s.field,
                            //^ u8
                }
            }
        }        
    """)

    fun `test infer match expr variant field destructuring rhs`() = testExpr("""
        module 0x1::m {
            enum S { One { field: u8 }, Two P }
            fun main(s: S) {
                match (s) {
                    One { field } => field,
                                       //^ u8
                }
            }
        }        
    """)

    fun `test infer match expr variant field destructuring rhs full`() = testExpr("""
        module 0x1::m {
            enum S { One { field: u8 }, Two }
            fun main(s: S) {
                match (s) {
                    One { field: myfield } => myfield,
                                             //^ u8
                }
            }
        }        
    """)

    fun `test infer match variant field with reference`() = testExpr("""
        module 0x1::m {
            struct Inner { field: u8 }
            enum Outer { None { inner: Inner } }
        
            public fun non_exhaustive(o: &Outer) {
                match (o) {
                    None { inner: myinner } => myinner
                                                //^ &0x1::m::Inner
                }
            }
        }        
    """)

    fun `test infer match variant field with reference shorthand`() = testExpr("""
        module 0x1::m {
            struct Inner { field: u8 }
            enum Outer { None { inner: Inner } }
        
            public fun non_exhaustive(o: &Outer) {
                match (o) {
                    None { inner } => inner
                                      //^ &0x1::m::Inner
                }
            }
        }        
    """)

    fun `test enum variant as value`() = testExpr("""
        module 0x1::m {
            enum Option { None }
            fun main() {
                let a = Option::None;
                a;
              //^ 0x1::m::Option  
            }
        }        
    """)

    fun `test enum variant struct as value with generics` () = testExpr("""
        module 0x1::m {
            enum Option<T> { Some { element: T } }
            fun main() {
                let a = Option::Some { element: 1u8 };
                a;
              //^ 0x1::m::Option<u8>  
            }
        }        
    """)

    fun `test index_of spec function is not resolved so integer parameter cannot be inferred`() = testExpr("""
        module 0x1::m {
            fun main() {
                let vect = vector[1u8];
                let ind = 1;
                index_of(vect, ind);
                ind;
                //^ integer
            }
        }        
    """)

    fun `test tuple result type of spec function with generic`() = testExpr("""
    module 0x1::m {
        struct Option<Element> has copy, drop, store {
            vec: vector<Element>
        }
        fun upsert(): Option<u8> { Option { vec: vector[1] } }
        spec upsert {
            result;
            //^ 0x1::m::Option<num>
        }
    }
    """)

    fun `test types for fields in tuple struct pattern`() = testExpr("""
        module 0x1::m {
            struct S(u8, u8);
            fun main(s: S) {
                let S ( field1, field2 ) = s;
                field1;
                //^ u8
            }
        }        
    """)

    fun `test field type for tuple struct pattern if more than number of fields but field is there`() = testExpr("""
        module 0x1::m {
            struct S(u8);
            fun main(s: S) {
                let S ( field1, field2 ) = s;
                field1;
                //^ u8
            }
        }        
    """)

    fun `test unknown type for tuple struct pattern if more than number of fields`() = testExpr("""
        module 0x1::m {
            struct S(u8);
            fun main(s: S) {
                let S ( field1, field2 ) = s;
                field2;
                //^ <unknown>
            }
        }        
    """)

    fun `test type for tuple struct literal`() = testExpr("""
        module 0x1::m {
            struct S<T>(T);
            fun main() {
                let s = S(true);
                s; 
              //^ 0x1::m::S<bool>   
            }
        }        
    """)

    fun `test type for tuple struct literal with multiple type parameters`() = testExpr("""
        module 0x1::m {
            struct S<T, U>(T, U);
            fun main() {
                let s = S(true, 1u8);
                s; 
              //^ 0x1::m::S<bool, u8>   
            }
        }        
    """)

    fun `test type for tuple struct literal with multiple type parameters and explicit type`() = testExpr("""
        module 0x1::m {
            struct S<T>(T, T);
            fun main() {
                let s = S<u8>(1, 1);
                s; 
              //^ 0x1::m::S<u8>   
            }
        }        
    """)

    fun `test positional field lookup type`() = testExpr("""
        module 0x1::m {
            struct S(u8);
            fun main(s: S) {
                s.0;
                //^ u8
            }
        }        
    """)

    fun `test positional field lookup generic type`() = testExpr("""
        module 0x1::m {
            struct S<T>(T)
            fun main() {
                let s = S(true);
                s.0;
                //^ bool
            }
        }        
    """)

    fun `test enum variant for tuple struct literal`() = testExpr("""
        module 0x1::m {
            enum S<T> { One(T) }
            fun main() {
                let s = S::One(true);
                s; 
              //^ 0x1::m::S<bool>   
            }
        }        
    """)

    fun `test enum variant for tuple struct literal field`() = testExpr("""
        module 0x1::m {
            enum S<T> { One(T) }
            fun main() {
                let s = S::One(true);
                s.0; 
                //^ bool   
            }
        }        
    """)

    fun `test if expr return type`() = testExpr("""
        module 0x1::m {
            enum S1 { One, Two }
            fun main(s: S1) {
                let ret = s is One;
                ret;
              //^ bool 
            }
        }        
    """)
}
