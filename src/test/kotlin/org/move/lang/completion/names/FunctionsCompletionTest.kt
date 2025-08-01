package org.move.lang.completion.names

import org.move.utils.tests.completion.CompletionTestCase

class FunctionsCompletionTest : CompletionTestCase() {
    fun `test function call zero args`() = doSingleCompletion(
        """
        module 0x1::m {
            fun frobnicate() {}
            fun main() {
                frob/*caret*/
            }
        }
    """, """
        module 0x1::m {
            fun frobnicate() {}
            fun main() {
                frobnicate()/*caret*/
            }
        }
    """
    )

    fun `test function call one arg`() = doSingleCompletion(
        """
        module 0x1::m {
            fun frobnicate(a: u8) {}
            fun main() {
                frob/*caret*/
            }
        }
    """, """
        module 0x1::m {
            fun frobnicate(a: u8) {}
            fun main() {
                frobnicate(/*caret*/)
            }
        }
    """
    )

    fun `test function call with parens`() = doSingleCompletion(
        """
        module 0x1::m {
            fun frobnicate() {}
            fun main() {
                frob/*caret*/()
            }
        }
    """, """
        module 0x1::m {
            fun frobnicate() {}
            fun main() {
                frobnicate()/*caret*/
            }
        }
    """
    )

    fun `test function call with parens with arg`() = doSingleCompletion(
        """
        module 0x1::m {
            fun frobnicate(a: u8) {}
            fun main() {
                frob/*caret*/()
            }
        }
    """, """
        module 0x1::m {
            fun frobnicate(a: u8) {}
            fun main() {
                frobnicate(/*caret*/)
            }
        }
    """
    )

    fun `test generic function call with uninferrable type parameters`() = doSingleCompletion("""
        module 0x1::m {
            fun frobnicate<T>() {}
            fun main() {
                frob/*caret*/
            }
        }
    """, """
        module 0x1::m {
            fun frobnicate<T>() {}
            fun main() {
                frobnicate</*caret*/>()
            }
        }
    """)

    fun `test type parameters accessible in types completion`() = doSingleCompletion(
        """
        module 0x1::m {
            fun main<CoinType>() {
                let a: Coi/*caret*/
            }
        }
    """, """
        module 0x1::m {
            fun main<CoinType>() {
                let a: CoinType/*caret*/
            }
        }
    """
    )

    fun `test no function completion in type position`() = checkNoCompletion(
        """
        module 0x1::m {
            public fun create() {}
        }
        module 0x1::main {
            fun main(a: 0x1::n::cr/*caret*/) {}
        }
    """
    )

    fun `test public and public(friend) completions for module`() = completionFixture.checkContainsCompletion(
        """
        module 0x1::m {
            friend 0x1::main;
            public(friend) fun create_friend() {}
            public fun create() {}
        }
        module 0x1::main {
            fun main() {
                0x1::m::cr/*caret*/
            }
        }
    """, listOf("create", "create_friend"))

    fun `test public and public(script) completions for script`() = completionFixture.checkContainsCompletion(
        """
        module 0x1::m {
            public(script) fun create_script() {}
            public entry fun create_entry() {}
            public fun create() {}
        }
        script {
            fun main() {
                0x1::m::cr/*caret*/
            }
        }
    """, listOf("create", "create_script"))

    fun `test Self completion`() = completionFixture.checkContainsCompletion(
        """
        module 0x1::m {
            public(friend) fun create_friend() {}
            public(script) fun create_script() {}
            public fun create() {}
            fun create_private() {}
            
            fun main() {
                Self::/*caret*/
            }
        }
    """, listOf("create", "create_script", "create_friend", "create_private"))

    fun `test fq completion for use`() = doSingleCompletion("""
    module 0x1::m1 {
        public fun call() {}
    }
    module 0x1::m2 {
        use 0x1::m1::c/*caret*/
    }
    """, """
    module 0x1::m1 {
        public fun call() {}
    }
    module 0x1::m2 {
        use 0x1::m1::call/*caret*/
    }
    """)

    fun `test fq completion for reference expr`() = doSingleCompletion("""
    module 0x1::m1 {
        public fun call() {}
    }
    module 0x1::m2 {
        fun m() {
            0x1::m1::c/*caret*/
        }
    }
    """, """
    module 0x1::m1 {
        public fun call() {}
    }
    module 0x1::m2 {
        fun m() {
            0x1::m1::call()/*caret*/
        }
    }
    """)

    fun `test insert angle brackets for borrow_global if not inferrable from context`() = doSingleCompletion("""
    module 0x1::m {
        fun m() {
            let a = borrow_global_/*caret*/
        }
    }    
    """, """
    module 0x1::m {
        fun m() {
            let a = borrow_global_mut</*caret*/>()
        }
    }    
    """)

    fun `test do not insert () if completed before angle brackets`() = doSingleCompletion("""
    module 0x1::m {
        fun m() {
            borrow_global_m/*caret*/<u8>(@0x1);
        }
    }    
    """, """
    module 0x1::m {
        fun m() {
            borrow_global_mut</*caret*/u8>(@0x1);
        }
    }    
    """)

    fun `test function in path position with auto import`() = doSingleCompletion("""
    module 0x1::Signer {
        public fun address_of(s: &signer): address { @0x1 }
    }
    module 0x1::m {
        fun call() {
            let a = 1;
            address_o/*caret*/
        }
    }
    """, """
    module 0x1::Signer {
        public fun address_of(s: &signer): address { @0x1 }
    }
    module 0x1::m {
        use 0x1::Signer::address_of;

        fun call() {
            let a = 1;
            address_of(/*caret*/)
        }
    }
    """)

    fun `test test_only function completion in test_only scope`() = doSingleCompletion("""
    module 0x1::minter {
        #[test_only]
        public fun get_weekly() {}
    }    
    #[test_only]
    module 0x1::minterTests {
        use 0x1::minter::get/*caret*/
    }
    """, """
    module 0x1::minter {
        #[test_only]
        public fun get_weekly() {}
    }    
    #[test_only]
    module 0x1::minterTests {
        use 0x1::minter::get_weekly/*caret*/
    }
    """)

    fun `test test_only function completion in test_only use item scope`() = doSingleCompletion("""
    module 0x1::minter {
        #[test_only]
        public fun get_weekly() {}
    }    
    module 0x1::minterTests {
        #[test_only]
        use 0x1::minter::get/*caret*/
    }
    """, """
    module 0x1::minter {
        #[test_only]
        public fun get_weekly() {}
    }    
    module 0x1::minterTests {
        #[test_only]
        use 0x1::minter::get_weekly/*caret*/
    }
    """)

    fun `test do not add angle brackets if type is inferrable from context`() = doSingleCompletion("""
    module 0x1::Event {
        struct EventHandle<phantom E> {}

        struct MyEvent {}
        struct EventStore {
            my_events: EventHandle<MyEvent>
        }

        fun new_event_handle<E>(): EventHandle<E> { EventHandle<E> {} }
        fun call() {
            EventStore { my_events: new_eve/*caret*/ };
        }
    }
    """, """
    module 0x1::Event {
        struct EventHandle<phantom E> {}

        struct MyEvent {}
        struct EventStore {
            my_events: EventHandle<MyEvent>
        }

        fun new_event_handle<E>(): EventHandle<E> { EventHandle<E> {} }
        fun call() {
            EventStore { my_events: new_event_handle()/*caret*/ };
        }
    }
    """)

    fun `test add angle brackets if untyped let pattern`() = doSingleCompletion("""
    module 0x1::main {
        struct Coin<CoinType> {}
        fun withdraw<CoinType>(): Coin<CoinType> { Coin<CoinType> {} }
        fun main() {
            let a = with/*caret*/;
        }
    }    
    """, """
    module 0x1::main {
        struct Coin<CoinType> {}
        fun withdraw<CoinType>(): Coin<CoinType> { Coin<CoinType> {} }
        fun main() {
            let a = withdraw</*caret*/>();
        }
    }    
    """)

    fun `test no angle brackets for function with generic vector parameter`() = doSingleCompletion("""
        module 0x1::m {
            native public fun destroy_empty<Element>(v: vector<Element>);
            fun main() {
                destroy/*caret*/
            }
        }        
    """, """
        module 0x1::m {
            native public fun destroy_empty<Element>(v: vector<Element>);
            fun main() {
                destroy_empty(/*caret*/)
            }
        }        
    """)


    fun `test complete function from module alias`() = doSingleCompletion("""
    module 0x1::string {
        public fun call() {}
    }    
    module 0x1::main {
        use 0x1::string as mystring;
        fun main() {
            mystring::ca/*caret*/
        }
    }    
    """, """
    module 0x1::string {
        public fun call() {}
    }    
    module 0x1::main {
        use 0x1::string as mystring;
        fun main() {
            mystring::call()/*caret*/
        }
    }    
    """)

    fun `test function completion at the left of equality expr`() = doSingleCompletion(
        """
        module 0x1::m {
            spec fun spec_some(a: u8): u8 { a }
            spec module {
                let a = 1;
                let b = 1;
                spec_/*caret*/ == spec_some(b);
            }
        }        
    """,
        """
        module 0x1::m {
            spec fun spec_some(a: u8): u8 { a }
            spec module {
                let a = 1;
                let b = 1;
                spec_some(/*caret*/) == spec_some(b);
            }
        }        
    """,
        )

    fun `test function completion at the right of equality expr`() = doSingleCompletion(
        """
        module 0x1::m {
            spec fun spec_some(a: u8): u8 { a }
            spec module {
                let a = 1;
                let b = 1;
                spec_some(a) == spec_/*caret*/;
            }
        }        
    """,
        """
        module 0x1::m {
            spec fun spec_some(a: u8): u8 { a }
            spec module {
                let a = 1;
                let b = 1;
                spec_some(a) == spec_some(/*caret*/);
            }
        }        
    """,
        )
}
