package org.move.lang.completion.names

import org.move.utils.tests.completion.CompletionTestCase

class FunctionsCompletionTest : CompletionTestCase() {
    fun `test function call zero args`() = doSingleCompletion(
        """
        module 0x1::M {
            fun frobnicate() {}
            fun main() {
                frob/*caret*/
            }
        }
    """, """
        module 0x1::M {
            fun frobnicate() {}
            fun main() {
                frobnicate()/*caret*/
            }
        }
    """
    )

    fun `test function call one arg`() = doSingleCompletion(
        """
        module 0x1::M {
            fun frobnicate(a: u8) {}
            fun main() {
                frob/*caret*/
            }
        }
    """, """
        module 0x1::M {
            fun frobnicate(a: u8) {}
            fun main() {
                frobnicate(/*caret*/)
            }
        }
    """
    )

    fun `test function call with parens`() = doSingleCompletion(
        """
        module 0x1::M {
            fun frobnicate() {}
            fun main() {
                frob/*caret*/()
            }
        }
    """, """
        module 0x1::M {
            fun frobnicate() {}
            fun main() {
                frobnicate()/*caret*/
            }
        }
    """
    )

    fun `test function call with parens with arg`() = doSingleCompletion(
        """
        module 0x1::M {
            fun frobnicate(a: u8) {}
            fun main() {
                frob/*caret*/()
            }
        }
    """, """
        module 0x1::M {
            fun frobnicate(a: u8) {}
            fun main() {
                frobnicate(/*caret*/)
            }
        }
    """
    )

    fun `test generic function call with uninferrable type parameters`() = doSingleCompletion("""
        module 0x1::M {
            fun frobnicate<T>() {}
            fun main() {
                frob/*caret*/
            }
        }
    """, """
        module 0x1::M {
            fun frobnicate<T>() {}
            fun main() {
                frobnicate</*caret*/>()
            }
        }
    """)

    fun `test type parameters accessible in types completion`() = doSingleCompletion(
        """
        module 0x1::M {
            fun main<CoinType>() {
                let a: Coi/*caret*/
            }
        }
    """, """
        module 0x1::M {
            fun main<CoinType>() {
                let a: CoinType/*caret*/
            }
        }
    """
    )

    fun `test no function completion in type position`() = checkNoCompletion(
        """
        address 0x1 {
        module Transaction {
            public fun create() {}
        }
        }
        
        module 0x1::M {
            fun main(a: 0x1::Transaction::cr/*caret*/) {}
        }
    """
    )

    fun `test public and public(friend) completions for module`() = completionFixture.checkContainsCompletion(
        """
        address 0x1 {
        module Transaction {
            friend 0x1::M;
            public(friend) fun create_friend() {}
            public fun create() {}
        }
        }
        
        module 0x1::M {
            fun main() {
                0x1::Transaction::cr/*caret*/
            }
        }
    """, listOf("create", "create_friend"))

    fun `test public and public(script) completions for script`() = completionFixture.checkContainsCompletion(
        """
        address 0x1 {
        module Transaction {
            friend 0x1::M;
            public(script) fun create_script() {}
            public fun create() {}
        }
        }
        
        script {
            fun main() {
                0x1::Transaction::cr/*caret*/
            }
        }
    """, listOf("create", "create_script"))

    fun `test Self completion`() = completionFixture.checkContainsCompletion(
        """
        address 0x1 {
        module Transaction {
            friend 0x1::M;
            public(friend) fun create_friend() {}
            public(script) fun create_script() {}
            public fun create() {}
            fun create_private() {}
            
            fun main() {
                Self::/*caret*/
            }
        }
        }
    """, listOf("create", "create_script", "create_friend", "create_private"))

    fun `test fq completion for use`() = doSingleCompletion("""
    module 0x1::M1 {
        public fun call() {}
    }
    module 0x1::M2 {
        use 0x1::M1::c/*caret*/
    }
    """, """
    module 0x1::M1 {
        public fun call() {}
    }
    module 0x1::M2 {
        use 0x1::M1::call/*caret*/
    }
    """)

    fun `test fq completion for reference expr`() = doSingleCompletion("""
    module 0x1::M1 {
        public fun call() {}
    }
    module 0x1::M2 {
        fun m() {
            0x1::M1::c/*caret*/
        }
    }
    """, """
    module 0x1::M1 {
        public fun call() {}
    }
    module 0x1::M2 {
        fun m() {
            0x1::M1::call()/*caret*/
        }
    }
    """)

    fun `test insert angle brackets for borrow_global if not inferrable from context`() = doSingleCompletion("""
    module 0x1::M {
        fun m() {
            let a = borrow_global_/*caret*/
        }
    }    
    """, """
    module 0x1::M {
        fun m() {
            let a = borrow_global_mut</*caret*/>()
        }
    }    
    """)

    fun `test do not insert () if completed before angle brackets`() = doSingleCompletion("""
    module 0x1::M {
        fun m() {
            borrow_global_m/*caret*/<u8>(@0x1);
        }
    }    
    """, """
    module 0x1::M {
        fun m() {
            borrow_global_mut</*caret*/u8>(@0x1);
        }
    }    
    """)

    fun `test no assert completion from module`() = checkNoCompletion("""
    module 0x1::M {}
    module 0x1::M2 {
        use 0x1::M;
        fun m() {
            M::asse/*caret*/
        }
    }
    """)

    fun `test function in path position with auto import`() = doSingleCompletion("""
    module 0x1::Signer {
        public fun address_of(s: &signer): address { @0x1 }
    }
    module 0x1::M {
        fun call() {
            let a = 1;
            address_o/*caret*/
        }
    }
    """, """
    module 0x1::Signer {
        public fun address_of(s: &signer): address { @0x1 }
    }
    module 0x1::M {
        use 0x1::Signer::address_of;

        fun call() {
            let a = 1;
            address_of(/*caret*/)
        }
    }
    """)

    fun `test test_only function completion in test_only scope`() = doSingleCompletion("""
    module 0x1::Minter {
        #[test_only]
        public fun get_weekly() {}
    }    
    #[test_only]
    module 0x1::MinterTests {
        use 0x1::Minter::get/*caret*/
    }
    """, """
    module 0x1::Minter {
        #[test_only]
        public fun get_weekly() {}
    }    
    #[test_only]
    module 0x1::MinterTests {
        use 0x1::Minter::get_weekly/*caret*/
    }
    """)

    fun `test test_only function completion in test_only use item scope`() = doSingleCompletion("""
    module 0x1::Minter {
        #[test_only]
        public fun get_weekly() {}
    }    
    module 0x1::MinterTests {
        #[test_only]
        use 0x1::Minter::get/*caret*/
    }
    """, """
    module 0x1::Minter {
        #[test_only]
        public fun get_weekly() {}
    }    
    module 0x1::MinterTests {
        #[test_only]
        use 0x1::Minter::get_weekly/*caret*/
    }
    """)

//    fun `test do not add angle brackets if type is inferrable from context`() = doSingleCompletion("""
//    module 0x1::Event {
//        struct EventHandle<phantom E> {}
//
//        struct MyEvent {}
//        struct EventStore {
//            my_events: EventHandle<MyEvent>
//        }
//
//        fun new_event_handle<E>(): EventHandle<E> { EventHandle<E> {} }
//        fun call() {
//            EventStore { my_events: new_eve/*caret*/ };
//        }
//    }
//    """, """
//    module 0x1::Event {
//        struct EventHandle<phantom E> {}
//
//        struct MyEvent {}
//        struct EventStore {
//            my_events: EventHandle<MyEvent>
//        }
//
//        fun new_event_handle<E>(): EventHandle<E> { EventHandle<E> {} }
//        fun call() {
//            EventStore { my_events: new_event_handle()/*caret*/ };
//        }
//    }
//    """)
}
