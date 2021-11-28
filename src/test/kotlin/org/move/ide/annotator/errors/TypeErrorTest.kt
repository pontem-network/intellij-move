package org.move.ide.annotator.errors

import org.move.ide.annotator.ErrorAnnotator
import org.move.utils.tests.annotation.AnnotatorTestCase

class TypeErrorTest: AnnotatorTestCase(ErrorAnnotator::class) {
    fun `test incorrect type address passed where &signer is expected`() = checkErrors("""
        module M {
            fun send(account: &signer) {}
            
            fun main(addr: address) {
                send(<error descr="Invalid argument for parameter 'account': type 'address' is not compatible with '&signer'">addr</error>)
            }
        }        
    """)

    fun `test incorrect type u8 passed where &signer is expected`() = checkErrors("""
        module M {
            fun send(account: &signer) {}
            
            fun main(addr: u8) {
                send(<error descr="Invalid argument for parameter 'account': type 'u8' is not compatible with '&signer'">addr</error>)
            }
        }        
    """)

    fun `test no errors if same type`() = checkErrors("""
        module M {
            fun send(account: &signer) {}
            
            fun main(acc: &signer) {
                send(acc)
            }
        }        
    """)

    fun `test mutable reference compatible with immutable reference`() = checkErrors("""
    module M {
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

    fun `test different generic types`() = checkErrors(
        """
    module M {
        struct Option<Element> {}
        fun is_none<Elem>(t: Option<u64>): bool {
            true
        }
        fun main() {
            let opt = Option<u8> {};
            is_none(<error descr="Invalid argument for parameter 't': type 'Option<u8>' is not compatible with 'Option<u64>'">opt</error>)
        } 
    }    
    """
    )

    fun `test different generic types for references`() = checkErrors(
        """
    module M {
        struct Option<Element> {}
        fun is_none<Elem>(t: &Option<u64>): bool {
            true
        }
        fun main() {
            let opt = &Option<u8> {};
            is_none(<error descr="Invalid argument for parameter 't': type '&Option<u8>' is not compatible with '&Option<u64>'">opt</error>)
        } 
    }    
    """
    )

    fun `test immutable reference is not compatible with mutable reference`() = checkErrors("""
    module M {
        struct Option<Element> {
            vec: vector<Element>
        }
        fun is_none<Element>(t: &mut Option<Element>): bool {
            true
        }
        fun main<Element>(opt: &Option<Element>) {
            is_none(<error descr="Invalid argument for parameter 't': type '&Option<Element>' is not compatible with '&mut Option<Element>'">opt</error>)
        } 
    }    
    """)

    fun `test incorrect type of argument with struct literal`() = checkErrors("""
    module M {
        struct A {}
        struct B {}
        
        fun use_a(a: A) {}
        fun main() {
            use_a(<error descr="Invalid argument for parameter 'a': type 'B' is not compatible with 'A'">B {}</error>)            
        }
    }
    """)

    fun `test incorrect type of argument with call expression`() = checkErrors("""
    module M {
        struct A {}
        struct B {}
        
        fun use_a(a: A) {}
        fun get_b(): B { B {} }
        
        fun main() {
            use_a(<error descr="Invalid argument for parameter 'a': type 'B' is not compatible with 'A'">get_b()</error>)            
        }
    }
    """)

    fun `test incorrect type of argument with call expression from different module`() = checkErrors("""
address 0x1 {
    module Other {
        struct B {}
        public fun get_b(): B { B {} }
    }
    module M {
        use 0x1::Other::get_b;
        
        struct A {}
        fun use_a(a: A) {}
        
        fun main() {
            use_a(<error descr="Invalid argument for parameter 'a': type '0x1::Other::B' is not compatible with 'A'">get_b()</error>)            
        }
    }
}
    """)

    fun `test bytearray is vector of u8`() = checkErrors("""
        module M {
            fun send(a: vector<u8>) {}
            fun main() {
                let a = b"deadbeef";
                send(a)
            }
        }        
    """)

    fun `test no error for compatible generic with explicit parameter`() = checkErrors("""
    module M {
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
    """)

    fun `test no error for compatible generic with inferred parameter`() = checkErrors("""
    module M {
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
    """)

    fun `test no return type but returns u8`() = checkErrors("""
    module M {
        fun call() {
            <error descr="Invalid return type: expected '()', found 'integer'">return 1</error>;
        }
    }    
    """)

    fun `test no return type but returns u8 with expression`() = checkErrors("""
    module M {
        fun call() {
            <error descr="Invalid return type: expected '()', found 'integer'">1</error>
        }
    }    
    """)

    fun `test if statement returns ()`() = checkErrors("""
    module M {
        fun m() {
            if (true) {1} else {2};
        }
    }    
    """)

    fun `test block expr returns ()`() = checkErrors("""
    module M {
        fun m() {
            {1};
        }
    }    
    """)

    fun `test error on code block if empty block and return type`() = checkErrors(
        """
    module M {
        fun call(): u8 {<error descr="Invalid return type: expected 'u8', found '()'">}</error>
    }    
        """
    )

    fun `test vector push back`() = checkErrors(
        """
    module 0x1::M {
        native public fun push_back<Element>(v: &mut vector<Element>, e: Element);
        
        fun m<E: drop>(v: &mut vector<E>, x: E): u8 {
            <error descr="Invalid return type: expected 'u8', found '()'">push_back(v, x)</error>
        }
    }    
    """
    )

    fun `test if condition should be boolean`() = checkErrors("""
    module M {
        fun m() {
            if (<error descr="Incompatible type 'integer', expected 'bool'">1</error>) 1;
        }
    }    
    """)

    fun `test incompatible types from branches`() = checkErrors("""
    module M {
        fun m() {
            if (true) {1} else {<error descr="Incompatible type 'bool', expected 'integer'">true</error>};
        }
    }    
    """)

    fun `test no type error with explicit generic`() = checkErrors(
        """
    module 0x1::M {
        struct Vault<VaultContent: store> has key {
            content: VaultContent
        }
        public fun new<Content: store>(owner: &signer,  content: Content) {
            move_to<Vault<Content>>(
                owner,
                Vault { content }
            )
        }
    }    
    """
    )
}
