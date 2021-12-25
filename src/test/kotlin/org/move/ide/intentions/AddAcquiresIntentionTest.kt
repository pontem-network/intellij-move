package org.move.ide.intentions

import org.move.utils.tests.MvIntentionTestCase

class AddAcquiresIntentionTest: MvIntentionTestCase(AddAcquiresIntention::class) {
    fun `test unavailable on simple function`() = doUnavailableTest("""
    module 0x1::M {
        fun main() {
            call/*caret*/();
        }
    }    
    """)

    fun `test unavailable on move_from without type argument`() = doUnavailableTest("""
    module 0x1::M {
        struct Loan has key {}
        fun main() {
            move_from/*caret*/(@0x1);
        }
    }    
    """)

    fun `test unavailable if unresolved type`() = doUnavailableTest("""
    module 0x1::M {
        fun main() {
            move_from<Loan>/*caret*/(@0x1);
        }
    }    
    """)

    fun `test unavailable on move_from with proper acquires type`() = doUnavailableTest("""
    module 0x1::M {
        struct Loan has key {}
        fun main() acquires Loan {
            move_from<Loan>/*caret*/(@0x1);
        }
    }    
    """)

    fun `test available on move_from without acquires`() = doAvailableTest("""
    module 0x1::M {
        struct Loan has key {}
        fun main() {
            move_from<Loan>/*caret*/(@0x1);
        }
    }    
    """, """
    module 0x1::M {
        struct Loan has key {}
        fun main() acquires Loan {
            move_from<Loan>/*caret*/(@0x1);
        }
    }    
    """)

    fun `test available on move_from fully qualified path`() = doAvailableTest("""
    module 0x1::Loans {
        struct Loan has key {}
    }    
    module 0x1::M {
        fun main() {
            move_from<0x1::Loans::Loan>/*caret*/(@0x1);
        }
    }
    """, """
    module 0x1::Loans {
        struct Loan has key {}
    }    
    module 0x1::M {
        fun main() acquires 0x1::Loans::Loan {
            move_from<0x1::Loans::Loan>/*caret*/(@0x1);
        }
    }
    """)

    fun `test available on move_from with different acquires`() = doAvailableTest("""
    module 0x1::M {
        struct Loan has key {}
        struct Deal has key {}
        fun main() acquires Deal {
            move_from<Loan>/*caret*/(@0x1);
        }
    }    
    """, """
    module 0x1::M {
        struct Loan has key {}
        struct Deal has key {}
        fun main() acquires Deal, Loan {
            move_from<Loan>/*caret*/(@0x1);
        }
    }    
    """)

    fun `test add acquires if present with generic`() = doAvailableTest("""
    module 0x1::M {
        struct CapState<phantom Feature> has key {}
        fun main<Feature>() {
            move_from<CapState<Feature>>/*caret*/(@0x1);
        }  
    }    
    """, """
    module 0x1::M {
        struct CapState<phantom Feature> has key {}
        fun main<Feature>() acquires CapState {
            move_from<CapState<Feature>>/*caret*/(@0x1);
        }  
    }    
    """)

    fun `test not available transitively in script`() = doUnavailableTest("""
    module 0x1::M {
        struct Loan {}
        public fun call() acquires Loan {}
    }    
    script {
        fun main() {
            0x1::M::call/*caret*/();
        }
    }
    """)

    fun `test available transitively in module`() = doAvailableTest("""
    module 0x1::M {
        struct Loan {}
        public fun call() acquires Loan {}
        fun main() {
            0x1::M::call/*caret*/();
        }
    }
    """, """
    module 0x1::M {
        struct Loan {}
        public fun call() acquires Loan {}
        fun main() acquires Loan {
            0x1::M::call/*caret*/();
        }
    }
    """)
}
