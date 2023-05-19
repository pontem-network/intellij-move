package org.move.ide.inspections

import org.move.utils.tests.DevelopmentMode
import org.move.utils.tests.annotation.InspectionTestBase

class MvTypeCheckInspectionTest : InspectionTestBase(MvTypeCheckInspection::class) {
    fun `test incorrect type address passed where &signer is expected`() = checkErrors(
        """
module 0x1::M {
    fun send(account: &signer) {}
    
    fun main(addr: address) {
        send(<error descr="Incompatible type 'address', expected '&signer'">addr</error>);
    }
} 
    """
    )

    fun `test incorrect type u8 passed where &signer is expected`() = checkErrors(
        """
module 0x1::M {
    fun send(account: &signer) {}
    
    fun main(addr: u8) {
        send(<error descr="Incompatible type 'u8', expected '&signer'">addr</error>)
    }
} 
    """
    )

    fun `test no errors if same type`() = checkErrors(
        """
        module 0x1::M {
            fun send(account: &signer) {}
            
            fun main(acc: &signer) {
                send(acc)
            }
        }        
    """
    )

    fun `test mutable reference compatible with immutable reference`() = checkErrors(
        """
    module 0x1::M {
        struct Option<Element> {
            vec: vector<Element>
        }
        fun is_none<Element>(t: &Option<Element>): bool {
            true
        }
        fun main(opt: &mut Option<Element>) {
            is_none(opt);
        } 
    }    
    """
    )

    fun `test same struct but different generic types`() = checkErrors(
        """
module 0x1::M {
    struct Option<Element> {}
    fun is_none<Elem>(t: Option<u64>): bool {
        true
    }
    fun main() {
        let opt = Option<u8> {};
        is_none(<error descr="Incompatible type 'Option<u8>', expected 'Option<u64>'">opt</error>);
    } 
}    
    """
    )

    fun `test different generic types for references`() = checkErrors(
        """
module 0x1::M {
    struct Option<Element> {}
    fun is_none<Elem>(t: &Option<u64>): bool {
        true
    }
    fun main() {
        let opt = &Option<u8> {};
        is_none(<error descr="Incompatible type '&Option<u8>', expected '&Option<u64>'">opt</error>);
    } 
}
    """
    )

    fun `test immutable reference is not compatible with mutable reference`() = checkErrors(
        """
module 0x1::M {
    struct Option<Element> {
        vec: vector<Element>
    }
    fun is_none<Element>(t: &mut Option<Element>): bool {
        true
    }
    fun main<Element>(opt: &Option<Element>) {
        is_none(<error descr="Incompatible type '&Option<Element>', expected '&mut Option<Element>'">opt</error>);
    } 
}    
    """
    )

    fun `test incorrect type of argument with struct literal`() = checkErrors(
        """
module 0x1::M {
    struct A {}
    struct B {}
    
    fun use_a(a: A) {}
    fun main() {
        use_a(<error descr="Incompatible type 'B', expected 'A'">B {}</error>)            
    }
}
    """
    )

    fun `test incorrect type of argument with call expression`() = checkErrors(
        """
module 0x1::M {
    struct A {}
    struct B {}
    
    fun use_a(a: A) {}
    fun get_b(): B { B {} }
    
    fun main() {
        use_a(<error descr="Incompatible type 'B', expected 'A'">get_b()</error>)            
    }
}
    """
    )

    fun `test incorrect type of argument with call expression from different module`() = checkErrors(
        """
module 0x1::Other {
    struct B {}
    public fun get_b(): B { B {} }
}
module 0x1::M {
    use 0x1::Other::get_b;
    
    struct A {}
    fun use_a(a: A) {}
    
    fun main() {
        use_a(<error descr="Incompatible type 'B', expected 'A'">get_b()</error>)            
    }
}
    """
    )

    fun `test bytearray is vector of u8`() = checkErrors(
        """
        module 0x1::M {
            fun send(a: vector<u8>) {}
            fun main() {
                let a = b"deadbeef";
                send(a)
            }
        }        
    """
    )

    fun `test no error for compatible generic with explicit parameter`() = checkErrors(
        """
    module 0x1::M {
        struct Diem<CoinType> has store { val: u64 }
        struct Balance<Token> has key {
            coin: Diem<Token>
        }
        
        fun value<CoinType: store>(coin: &Diem<CoinType>) {}
        
        fun main<Token: store>() {
            let balance: Balance<Token>;
            let coin = &balance.coin;
            value<Token>(coin)
        }
    }        
    """
    )

    fun `test no error for compatible generic with inferred parameter`() = checkErrors(
        """
    module 0x1::M {
        struct Diem<CoinType> has store { val: u64 }
        struct Balance<Token> has key {
            coin: Diem<Token>
        }
        
        fun value<CoinType: store>(coin: &Diem<CoinType>) {}
        
        fun main<Token: store>() {
            let balance: Balance<Token>;
            let coin = &balance.coin;
            value(coin)
        }
    }        
    """
    )

    fun `test no return type but returns u8`() = checkErrors(
        """
    module 0x1::M {
        fun call() {
            return <error descr="Incompatible type 'integer', expected '()'">1</error>;
        }
    }    
    """
    )

    fun `test no return type but returns u8 with expression`() = checkErrors(
        """
    module 0x1::M {
        fun call() {
            <error descr="Incompatible type 'integer', expected '()'">1</error>
        }
    }    
    """
    )

    fun `test if statement returns ()`() = checkErrors(
        """
    module 0x1::M {
        fun m() {
            if (true) {1} else {2};
        }
    }    
    """
    )

    fun `test block expr returns ()`() = checkErrors(
        """
    module 0x1::M {
        fun m() {
            {1};
        }
    }    
    """
    )

    fun `test error on code block if empty block and return type`() = checkErrors(
        """
    module 0x1::M {
        fun call(): u8 {<error descr="Incompatible type '()', expected 'u8'">}</error>
    }    
        """
    )

    fun `test vector push back`() = checkErrors(
        """
    module 0x1::M {
        native public fun push_back<Element>(v: &mut vector<Element>, e: Element);
        
        fun m<E: drop>(v: &mut vector<E>, x: E): u8 {
            <error descr="Incompatible type '()', expected 'u8'">push_back(v, x)</error>
        }
    }    
    """
    )

    fun `test if condition should be boolean`() = checkErrors(
        """
    module 0x1::M {
        fun m() {
            if (<error descr="Incompatible type 'integer', expected 'bool'">1</error>) 1;
        }
    }    
    """
    )

    fun `test incompatible types from branches`() = checkErrors(
        """
    module 0x1::M {
        fun m() {
            if (true) {1} else <error descr="Incompatible type 'bool', expected 'integer'">{true}</error>;
        }
    }    
    """
    )

    fun `test no type error with explicit generic as move_to`() = checkErrors(
        """
    module 0x1::M {
        struct Option<Element: store> has store {
            element: Element
        }
        public fun some<SomeElement: store>(e: SomeElement): Option<SomeElement> {
            Option { element: e }
        }
        struct Vault<VaultContent: store> has key {
            content: Option<VaultContent>
        }
        public fun new<Content: store>(owner: &signer,  content: Content) {
            move_to<Vault<Content>>(
                owner,
                Vault { content: some(content) }
            )
        }
    }    
    """
    )

    fun `test type check incompatible constraints`() = checkErrors(
        """
module 0x1::M {
    struct C {}
    struct D {}
    fun new<Content>(a: Content, b: Content): Content { a }
    fun m() {
        new(C {}, <error descr="Incompatible type 'D', expected 'C'">D {}</error>);
    }
}
    """
    )

    fun `test error if resolved type requires a reference`() = checkErrors(
        """
module 0x1::M {
    fun index_of<Element>(v: &vector<Element>, e: &Element): (bool, u64) {
        (false, 0)
    }
    fun m() {
        let ids: vector<u64>;
        index_of(&ids, <error descr="Incompatible type 'u64', expected '&u64'">1u64</error>);
    }
}    
    """
    )

    fun `test return generic tuple from nested callable`() = checkErrors(
        """
    module 0x1::M {
        struct MintCapability<phantom CoinType> has key, store {}
        struct BurnCapability<phantom CoinType> has key, store {}

        public fun register_native_currency<FCoinType>(): (MintCapability<FCoinType>, BurnCapability<FCoinType>) {
            register_currency<FCoinType>()
        }
        public fun register_currency<FCoinType>(): (MintCapability<FCoinType>, BurnCapability<FCoinType>) {
            return (MintCapability<FCoinType>{}, BurnCapability<FCoinType>{})
        }
    }    
    """
    )

    fun `test emit event requires mutable reference error`() = checkErrors(
        """
module 0x1::M {
    struct EventHandle<phantom T: drop + store> has store {
        counter: u64,
        guid: vector<u8>,
    }
    struct Account has key {
        handle: EventHandle<Event>
    }
    struct Event has store, drop {}
    fun emit_event<T: drop + store>(handler_ref: &mut EventHandle<T>, msg: T) {}
    fun m<Type: store + drop>() acquires Account {
        emit_event(<error descr="Incompatible type 'EventHandle<Event>', expected '&mut EventHandle<Event>'">borrow_global_mut<Account>(@0x1).handle</error>, Event {});
    }
    
}    
    """
    )

    fun `test invalid type for field in struct literal`() = checkErrors(
        """
module 0x1::M {
    struct Deal { val: u8 }
    fun main() {
        Deal { val: <error descr="Incompatible type 'bool', expected 'u8'">false</error> };
    }
}    
"""
    )

    fun `test valid type for field`() = checkErrors(
        """
    module 0x1::M {
        struct Deal { val: u8 }
        fun main() {
            Deal { val: 10 };
            Deal { val: 10u8 };
        }
    }    
    """
    )

    fun `test no need for explicit type parameter if inferrable from context`() = checkErrors(
        """
    module 0x1::M {
        struct Option<Element> has copy, drop, store {}
        public fun none<NoneElement>(): Option<NoneElement> {
            Option {}
        }
        struct S { field: Option<address> }
        fun m(): S {
            S { field: none() }
        }
        
    }
    """
    )

    fun `test no need for vector empty() generic`() = checkErrors(
        """
    module 0x1::M {
        /// Create an empty vector.
        native public fun empty<Element>(): vector<Element>;
        struct CapState<phantom Feature> has key {
            delegates: vector<address>
        }
        fun m() {
            CapState { delegates: empty() };
        }
    }    
    """
    )

    fun `test type error in struct literal field shorthand`() = checkErrors(
        """
    module 0x1::M {
        struct S { a: u8 }
        fun m() {
            let a = true;
            S { <error descr="Incompatible type 'bool', expected 'u8'">a</error> };
        }
    }    
    """
    )

    fun `test do not crash type checking invalid number of type params or call params`() = checkErrors(
        """
    module 0x1::M {
        struct S<R: key> { val: R }
        fun call(a: u8) {}
        fun m() {
            let s = S<u8, u8>{};
            call(1, 2, 3);
        }
    }    
    """
    )

    fun `test explicit unit return`() = checkErrors(
        """
    module 0x1::M {
        fun m(): () {}
    }    
    """
    )

    fun `test if else with references no error if coerceable`() = checkErrors(
        """
    module 0x1::M {
        struct S {}
        fun m(s: &S, s_mut: &mut S) {
            (if (cond) s_mut else s);
        }
    }    
    """
    )

    fun `test incorrect type address passed where &signer is expected in spec`() = checkErrors(
        """
module 0x1::M {
    fun send(account: &signer) {}
    
    spec send {
        send(<error descr="Incompatible type 'address', expected '&signer'">@0x1</error>);
    }
}   
    """
    )

    fun `test signer compatibility in spec`() = checkErrors(
        """
    module 0x1::M {
        fun address_of(account: &signer): address { @0x1 }
        fun send(account: &signer) {}
        spec send {
            address_of(account);
        }
    }    
    """
    )

    fun `test vector_u8 is compatible with vector_num inside spec`() = checkErrors(
        """
    module 0x1::M {
        struct S { 
            val: vector<u8> 
        }       
        spec module {
            S { val: b"" };
        }
    }    
    """
    )

    fun `test ref equality for generics in spec call expr`() = checkErrors(
        """
    module 0x1::M {
        struct Token<TokenT> {}
        fun call<TokenT>(ref: &Token<TokenT>) {
            let token = Token<TokenT> {};    
            spec {
                call(token);
            }
        }
    }    
    """
    )

    fun `test invalid argument to plus expr`() = checkErrors(
        """
    module 0x1::M {
        fun add(a: bool, b: bool) {
            <error descr="Invalid argument to '+': expected integer type, but found 'bool'">a</error> 
            + <error descr="Invalid argument to '+': expected integer type, but found 'bool'">b</error>;
        }
    }    
    """
    )

    fun `test invalid argument to minus expr`() = checkErrors(
        """
    module 0x1::M {
        fun add(a: bool, b: bool) {
            <error descr="Invalid argument to '-': expected integer type, but found 'bool'">a</error> 
            - <error descr="Invalid argument to '-': expected integer type, but found 'bool'">b</error>;
        }
    }    
    """
    )

    fun `test invalid argument to plus expr for type parameter`() = checkErrors(
        """
    module 0x1::M {
        fun add<T>(a: T, b: T) {
            <error descr="Invalid argument to '+': expected integer type, but found 'T'">a</error> 
            + <error descr="Invalid argument to '+': expected integer type, but found 'T'">b</error>;
        }
    }    
    """
    )

    fun `test no error if return nested in if and while`() = checkErrors(
        """
    module 0x1::M {
        fun main(): u8 {
            let i = 0;
            while (true) {
                if (true) return i
            };
            i
        }
    }    
    """
    )

    fun `test no error empty return`() = checkErrors(
        """
    module 0x1::M {
        fun main() {
            if (true) return
            return 
        }
    }    
    """
    )

    fun `test no error return tuple from if else`() = checkErrors(
        """
    module 0x1::M {
        fun main(): (u8, u8) {
            if (true) {
                return (1, 1) 
            } else {
                return (2, 2)
            }
        }
    }    
    """
    )

    fun `test no error return tuple from nested if else`() = checkErrors(
        """
    module 0x1::M {
        fun main(): (u8, u8) {
            if (true) {
                if (true) {
                    return (1, 1) 
                } else {
                    return (2, 2)
                }
            } else {
                return (3, 3)
            }
        }
    }    
    """
    )

    fun `test error add to bool in assignment expr`() = checkErrors(
        """
    module 0x1::M {
        fun main() {
            let a = 1u64;
            let b = false;
            a = a + <error descr="Invalid argument to '+': expected integer type, but found 'bool'">b</error>;
        }
    }    
    """
    )

    fun `test error invalid assignment type`() = checkErrors(
        """
    module 0x1::M {
        fun main() {
            let a = 1u64;
            a = <error descr="Incompatible type 'bool', expected 'u64'">false</error>;
        }
    }    
    """
    )

    fun `test tuple unpacking with three elements when two is specified`() = checkErrors(
        """
    module 0x1::M {
        fun tuple(): (u8, u8, u8) { (1, 1, 1) }
        fun main() {
            let <error descr="Invalid unpacking. Expected tuple binding of length 3: (_, _, _)">(a, b)</error> = tuple();
        }
    }    
    """
    )

    fun `test tuple unpacking no nested errors`() = checkErrors(
        """
    module 0x1::M {
        struct S { val: u8 }
        fun tuple(): (u8, u8, u8) { (1, 1, 1) }
        fun main() {
            let <error descr="Invalid unpacking. Expected tuple binding of length 3: (_, _, _)">(S { val }, b)</error> = tuple();
        }
    }    
    """
    )

    fun `test tuple unpacking into struct when tuple pat is expected is specified`() = checkErrors(
        """
    module 0x1::M {
        struct S { val: u8 }
        fun tuple(): (u8, u8, u8) { (1, 1, 1) }
        fun main() {
            let <error descr="Invalid unpacking. Expected tuple binding of length 3: (_, _, _)">S { val }</error> = tuple();
        }
    }    
    """
    )

    fun `test unpacking struct into field`() = checkErrors(
        """
    module 0x1::M {
        struct S { val: u8 }
        fun s(): S { S { val: 10 } }
        fun main() {
            let s = s();
        }
    }    
    """
    )

    fun `test error unpacking struct into tuple`() = checkErrors(
        """
    module 0x1::M {
        struct S { val: u8 }
        fun s(): S { S { val: 10 } }
        fun main() {
            let <error descr="Invalid unpacking. Expected struct binding of type 0x1::M::S">(a, b)</error> = s();
        }
    }    
    """
    )

    fun `test error unpacking struct into struct when single var is expected`() = checkErrors(
        """
    module 0x1::M {
        struct S { val: u8 }
        fun s(): u8 { 1 }
        fun main() {
            let <error descr="Invalid unpacking. Expected a single variable">(a, b)</error> = s();
        }
    }    
    """
    )

    fun `test error parameter type with return type inferred`() = checkErrors(
        """
    module 0x1::M {
        fun identity<T>(a: T): T { a }
        fun main() {
            let a: u8 = identity(<error descr="Incompatible type 'u64', expected 'u8'">1u64</error>);
        }
    }        
    """
    )

    fun `test all integers are nums in spec blocks`() = checkErrors(
        """
    module 0x1::main {
        spec fun spec_pow(y: u64, x: u64): u64 {
            if (x == 0) {
                1
            } else {
                y * spec_pow(y, x - 1)
            }
        }

        /// Returns 10^degree.
        public fun pow_10(degree: u8): u64 {
            let res = 1;
            let i = 0;
            while ({
                spec {
                    invariant res == spec_pow(10, i);
                    invariant 0 <= i && i <= degree;
                };
                i < degree
            }) {
                res = res * 10;
                i = i + 1;
            };
            res
        }
    }        
    """
    )

    fun `test no error unpacking a struct from move_from`() = checkByText(
        """
module 0x1::main {
    struct Container has key { val: u8 }
    fun main() {
        let Container { val } = move_from(source_addr);
    }
}        
    """
    )

    fun `test vector lit with explicit type and type error`() = checkByText(
        """
module 0x1::main {
    fun main() {
        vector<u8>[<error descr="Incompatible type 'u64', expected 'u8'">1u64</error>];
    }
}        
    """
    )

    fun `test vector lit with implicit type and type error`() = checkByText(
        """
module 0x1::main {
    fun main() {
        vector[1u8, <error descr="Incompatible type 'u64', expected 'u8'">1u64</error>];
    }
}        
    """
    )

    fun `test call expr with incomplete arguments and explicit type`() = checkByText(
        """
    module 0x1::main {
        fun call<T>(a: T, b: T): T {
            b        
        }    
        fun main() {
            call<u8>(<error descr="Incompatible type 'u64', expected 'u8'">1u64</error>);
        }    
    }        
    """
    )

    fun `test call expr with incomplete arguments and implicit type`() = checkByText(
        """
    module 0x1::main {
        fun call<T>(a: T, b: T, c: T): T {
            b        
        }    
        fun main() {
            call(1u8, <error descr="Incompatible type 'u64', expected 'u8'">1u64</error>);
        }    
    }        
    """
    )

    fun `test option none is compatible with any option`() = checkByText(
        """
module 0x1::option {
    struct Option<Element: copy + drop + store> has copy, drop, store {
        vec: vector<Element>
    }
    public fun none<Element: copy + drop + store>(): Option<Element> {
        Option { vec: vector::empty() }
    }
}        
module 0x1::main {
    use 0x1::option;
    struct IterableValue<K: copy + store + drop> has store {
        prev: option::Option<K>,
        next: option::Option<K>,
    }
    public fun new() {
        IterableValue { prev: option::none(), next: option::none() };
    }
}        
    """
    )

    fun `test deeply nested structure type is unknown due to memory issues`() = checkByText(
        """
module 0x1::main {
    struct Box<T> has copy, drop, store { x: T }
    struct Box3<T> has copy, drop, store { x: Box<Box<T>> }
    struct Box7<T> has copy, drop, store { x: Box3<Box3<T>> }
    struct Box15<T> has copy, drop, store { x: Box7<Box7<T>> }
    struct Box31<T> has copy, drop, store { x: Box15<Box15<T>> }
    struct Box63<T> has copy, drop, store { x: Box31<Box31<T>> }
    
    fun box3<T>(x: T): Box3<T> {
        Box3 { x: Box { x: Box { x } } }
    }

    fun box7<T>(x: T): Box7<T> {
        Box7 { x: box3(box3(x)) }
    }

    fun box15<T>(x: T): Box15<T> {
        Box15 { x: box7(box7(x)) }
    }

    fun box31<T>(x: T): Box31<T> {
        Box31 { x: box15(box15(x)) }
    }
    
    fun box63<T>(x: T): Box63<T> {
        Box63 { x: box31(box31(x)) }
    }

    fun main() {
        let a: Box63<u8>;
        a;
      //^ unknown  
    }
}
    """
    )

    fun `test no invalid unpacking error for unresolved name tuple`() = checkByText(
        """
module 0x1::main {
    fun main() {
        let (a, b) = call();
    }
}        
    """
    )

    fun `test no invalid unpacking error for unresolved name struct`() = checkByText(
        """
module 0x1::main {
    struct S { val: u8 }
    fun main() {
        let S { val } = call();
    }
}        
    """
    )

    fun `test loop never returns and not a type error`() = checkByText(
        """
module 0x1::main {
    fun main(): u64 {
        let a = 1;
        if (a == 1) return a;
        loop {}
    }
}        
    """
    )

    fun `test integer arguments of the same type support ordering`() = checkByText(
        """
module 0x1::main {
    fun main(a: u64, b: u64) {
        let c = 1;
        a < b;
        a > b;
        a >= b;
        a <= b;
        a < c;
        b < c;
    }
}        
    """
    )

    fun `test cannot order references`() = checkByText(
        """
module 0x1::main {
    fun main(a: &u64, b: &u64) {
        <error descr="Invalid argument to '<': expected integer type, but found '&u64'">a</error> 
        < <error descr="Invalid argument to '<': expected integer type, but found '&u64'">b</error>;
    }
}        
    """
    )

    fun `test cannot order bools`() = checkByText(
        """
module 0x1::main {
    fun main(a: bool, b: bool) {
        <error descr="Invalid argument to '<': expected integer type, but found 'bool'">a</error> 
        < <error descr="Invalid argument to '<': expected integer type, but found 'bool'">b</error>;
    }
}        
    """
    )

    fun `test cannot order type parameters`() = checkByText(
        """
module 0x1::main {
    fun main<T>(a: T, b: T) {
        <error descr="Invalid argument to '<': expected integer type, but found 'T'">a</error> 
        < <error descr="Invalid argument to '<': expected integer type, but found 'T'">b</error>;
    }
}        
    """
    )

    fun `test equality is supported for the same type objects`() = checkByText(
        """
module 0x1::main {
    struct S { val: u8 }
    fun main<T>(a: T, b: T) {
        1 == 1;
        1u8 == 1u8;
        1u64 == 1u64;
        false == false;
        S { val: 10 } == S { val: 20 };
        a == b;
    }
}        
    """
    )

    fun `test inequality is supported for the same type objects`() = checkByText(
        """
module 0x1::main {
    struct S { val: u8 }
    fun main<T>(a: T, b: T) {
        1 != 1;
        1u8 != 1u8;
        1u64 != 1u64;
        false != false;
        S { val: 10 } != S { val: 20 };
        a != b;
    }
}        
    """
    )

    fun `test cannot equal completely different types`() = checkByText(
        """
module 0x1::main {
    struct S { val: u64 }
    fun main() {
        <error descr="Incompatible arguments to '==': 'integer' and 'bool'">1 == false</error>;
        <error descr="Incompatible arguments to '==': 'S' and 'bool'">S { val: 10 } == false</error>;
    }
}        
    """
    )

    fun `test cannot equal different integer types`() = checkByText(
        """
module 0x1::main {
    fun main() {
        <error descr="Incompatible arguments to '==': 'u8' and 'u64'">1u8 == 1u64</error>;
    }
}        
    """
    )

    fun `test cannot inequal different integer types`() = checkByText(
        """
module 0x1::main {
    fun main() {
        <error descr="Incompatible arguments to '!=': 'u8' and 'u64'">1u8 != 1u64</error>;
    }
}        
    """
    )

    fun `test logic expressions allow booleans`() = checkByText(
        """
module 0x1::main {
    fun main() {
        true && true;
        false || false;
    }
}        
    """
    )

    fun `test logic expressions invalid argument type`() = checkByText(
        """
module 0x1::main {
    fun main() {
        <error descr="Incompatible type 'u8', expected 'bool'">1u8</error> 
        && <error descr="Incompatible type 'u64', expected 'bool'">1u64</error>;
    }
}        
    """
    )

    fun `test if else with different generic parameters`() = checkByText(
        """
module 0x1::main {
    struct G<X, Y> {}
    fun main<X, Y>() {
        if (true) {
            G<X, Y> {}
        } else <error descr="Incompatible type 'G<Y, X>', expected 'G<X, Y>'">{
            G<Y, X> {}
        }</error>;
    }
}        
    """
    )

    fun `test type cannot contain itself`() = checkByText(
        """
module 0x1::main {
    struct S { val: <error descr="Circular reference of type 'S'">S</error> }
}        
    """
    )

    fun `test type cannot contain itself in vector`() = checkByText(
        """
module 0x1::main {
    struct S { val: vector<<error descr="Circular reference of type 'S'">S</error>> }
}        
    """
    )

    fun `test cannot sum up bool and u64`() = checkByText(
        """
module 0x1::main {
    fun main() {
        <error descr="Invalid argument to '+': expected integer type, but found 'bool'">false</error> + 1u64;
    }
}        
    """
    )

    fun `test cannot sum up u8 and u64`() = checkByText(
        """
module 0x1::main {
    fun main() {
        <error descr="Incompatible arguments to '+': 'u8' and 'u64'">1u8 + 1u64</error>;
    }
}        
    """
    )

    fun `test recursive structs`() = checkByText(
        """
module 0x42::M0 {
    struct Foo { f: <error descr="Circular reference of type 'Foo'">Foo</error> }

    struct Cup<T> { f: T }
    struct Bar { f: Cup<<error descr="Circular reference of type 'Bar'">Bar</error>> }

    struct X { y: vector<Y> }
    struct Y { x: vector<X> }

}

module 0x42::M1 {
    use 0x42::M0;

    struct Foo { f: M0::Cup<<error descr="Circular reference of type 'Foo'">Foo</error>> }

    struct A { b: B }
    struct B { c: C }
    struct C { d: vector<D> }
    struct D { x: M0::Cup<M0::Cup<M0::Cup<A>>> }
}
    """
    )

    fun `test no error for table borrow mut of unknown type`() = checkByText(
        """
module 0x1::table {
    /// Type of tables
    struct Table<phantom K: copy + drop, V: store> has store {
        inner: V 
    }
    public fun borrow_mut<K: copy + drop, V: store>(table: &mut Table<K, V>, key: K): &mut V {
        &mut table.inner
    }
}
module 0x1::pool {
    use 0x1::table;
    struct Pool {
        shares: Unknown
    }
    fun call(pool: &mut Pool) {
        let value = table::borrow_mut(&mut pool.shares, @0x1);
        let unref = *value;
        1u128 - unref;
    }
}        
    """
    )

    fun `test no error for nested struct literal and explicit type`() = checkByText(
        """
    module 0x1::M {
        struct Option<Element> { element: Element } 
        struct S { id: Option<u64> }

        fun m() {
            S { id: Option { element: 1u64 } };
        }
    }    
    """
    )

    fun `test if else with expected type`() = checkByText(
        """
        module 0x1::m {
            fun main() {
                let a = 1;
                a = <error descr="Incompatible type 'bool', expected 'integer'">if (true) false else true</error>;
            }
        }        
    """
    )

    fun `test uninitialized integer with binary expr`() = checkByText(
        """
        module 0x1::m {
            fun main() {
                let lt;
                if (true) {
                    lt = 1;
                } else {
                    lt = 2;
                };
                lt - 1;
            }
        }        
    """
    )

    fun `test no invalid unpacking for full struct pat`() = checkByText(
        """
        module 0x1::m {
            struct S<phantom CoinType> { amount: u8 }
            fun call<CallCoinType>(s: S<CallCoinType>) {
                let S { amount: my_amount } = s;
            }
        }        
    """
    )

    fun `test no invalid unpacking for shorthand struct pat`() = checkByText(
        """
        module 0x1::m {
            struct S<phantom CoinType> { amount: u8 }
            fun call<CallCoinType>(s: S<CallCoinType>) {
                let S { amount } = s;
            }
        }        
    """
    )

    fun `test no invalid unpacking for variable in parens`() = checkByText(
        """
        module 0x1::m {
            fun call() {
                let (a) = 1;
            }
        }        
    """
    )

    fun `test check type of assigning value in tuple assignment`() = checkByText(
        """
        module 0x1::m {
            struct Coin<CoinType> { val: u8 }
            fun coin_zero<CoinType>(): Coin<CoinType> { Coin { val: 0 } }
            fun call<CallCoinType>() {
                let a = 0;
                (a, _) = (<error descr="Incompatible type 'Coin<CallCoinType>', expected 'integer'">coin_zero<CallCoinType>()</error>, 2);
            }
        }        
    """
    )

    fun `test deref type error`() = checkByText(
        """
        module 0x1::m {
            fun main() {
                let a = &&mut 1;
                let b: bool = <error descr="Incompatible type 'integer', expected 'bool'">**a</error>;
            }        
        } 
    """
    )

    fun `test shift left with u64`() = checkByText(
        """
        module 0x1::m {
            fun main() {
                let a = 1u64;
                a << 1;
            }        
        } 
    """
    )

    fun `test abort expr requires an integer type`() = checkByText(
        """
        module 0x1::m {
            fun main() {
                abort 1;
                abort 1u8;
                abort 1u64;
                abort <error descr="Incompatible type 'bool', expected 'integer'">false</error>;
            }
        }        
    """
    )

    fun `test aborts if with requires an integer type`() = checkByText(
        """
        module 0x1::m {
            fun call() {}
            spec call {
                aborts_if true with 1;
                aborts_if true with 1u8;
                aborts_if true with 1u64;
                aborts_if true with <error descr="Incompatible type 'bool', expected 'integer'">false</error>;
            }
        }        
    """
    )

    fun `test aborts with requires integer`() = checkByText(
        """
        module 0x1::m {
            fun call() {}
            spec call {
                aborts_with <error descr="Incompatible type 'bool', expected 'integer'">false</error>;
            }
        }        
    """
    )

    fun `test type check function param in func spec`() = checkByText(
        """
        module 0x1::m {
            fun call(val: bool) {}
            spec call { 
                <error descr="Invalid argument to '+': expected integer type, but found 'bool'">val</error> + 1;
            }
        }        
    """
    )

    fun `test type check function result in func spec`() = checkByText(
        """
        module 0x1::m {
            fun call(): bool { true }
            spec call {
                <error descr="Invalid argument to '+': expected integer type, but found 'bool'">result</error> + 1;
            }
        }        
    """
    )

    fun `test spec vector slice`() = checkByText(
        """
            module 0x1::m {
                spec module {
                    let v = vector[true, false];
                    v[0..1];
                }
            }        
        """
    )

    fun `test type check imply expr in include`() = checkByText(
        """
        module 0x1::m {
            spec schema Schema {}
            spec module {
                include <error descr="Incompatible type 'num', expected 'bool'">1</error> ==> Schema {};
            } 
        }        
    """
    )

    fun `test incompatible integers to gte`() = checkByText("""
        module 0x1::m {
            fun main() {
                1u8 >= <error descr="Incompatible type 'u64', expected 'u8'">1u64</error>;
            }
        }        
    """)

    fun `test bit shift requires u8`() = checkByText("""
        module 0x1::m {
            fun main() {
                1 << <error descr="Incompatible type 'u64', expected 'u8'">1000u64</error>;
            }
        }        
    """)

    fun `test type check incomplete call expr get correct param`() = checkByText("""
        module 0x1::m {
            fun call(a: u64, b: u8) {}
            fun main() {
                call(<error descr="<expression> expected, got ','">,</error> <error descr="Incompatible type 'u64', expected 'u8'">2u64</error>);
            }
        }        
    """)
}
