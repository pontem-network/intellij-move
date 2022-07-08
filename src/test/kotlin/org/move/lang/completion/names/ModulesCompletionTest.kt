package org.move.lang.completion.names

import org.move.utils.tests.completion.CompletionTestCase

class ModulesCompletionTest : CompletionTestCase() {
    fun `test autocomplete imported modules in name position`() = doSingleCompletion(
        """
        script {
            use 0x1::Transaction;
            
            fun main() {
                let a = Tra/*caret*/
            }
        }    
    """, """
        script {
            use 0x1::Transaction;
            
            fun main() {
                let a = Transaction/*caret*/
            }
        }    
    """
    )

    fun `test autocomplete imported modules in type position`() = doSingleCompletion(
        """
        script {
            use 0x1::Transaction;
            
            fun main(a: Tra/*caret*/) {}
        }    
    """, """
        script {
            use 0x1::Transaction;
            
            fun main(a: Transaction/*caret*/) {}
        }    
    """
    )

    fun `test autocomplete available modules for address`() = checkContainsCompletion(
        "Transaction", """
        address 0x1 {
            module Transaction {}
        }
        
        script {
            use 0x01::/*caret*/
        }
        """
    )

    fun `test complete Self item`() = doSingleCompletion("""
    module 0x1::Signer {} 
    module 0x1::M {
        use 0x1::Signer::{Se/*caret*/};
    }    
    """, """
    module 0x1::Signer {} 
    module 0x1::M {
        use 0x1::Signer::{Self/*caret*/};
    }    
    """)

    fun `test no Self completion if already imported in this block`() = checkNoCompletion("""
    module 0x1::Signer {} 
    module 0x1::M {
        use 0x1::Signer::{Self, Se/*caret*/};
    }    
    """)

    fun `test no Self completion without block`() = checkNoCompletion("""
    module 0x1::Signer {} 
    module 0x1::M {
        use 0x1::Signer::Se/*caret*/;
    }    
    """)

    fun `test module name in path position with auto import`() = doSingleCompletion("""
    module 0x1::Signer {}
    module 0x1::M {
        fun call() {
            let a = 1;
            Sign/*caret*/
        }
    }    
    """, """
    module 0x1::Signer {}
    module 0x1::M {
        use 0x1::Signer;

        fun call() {
            let a = 1;
            Signer/*caret*/
        }
    }    
    """)

    fun `test no import if name already in scope`() = doSingleCompletion("""
    module 0x1::Signer {}
    module 0x1::M {
        use 0x1::Signer;

        fun call() {
            let a = 1;
            Sign/*caret*/
        }
    }    
    """, """
    module 0x1::Signer {}
    module 0x1::M {
        use 0x1::Signer;

        fun call() {
            let a = 1;
            Signer/*caret*/
        }
    }    
    """)

    fun `test module name itself should not be present in completion`() = checkNoCompletion("""
    module 0x1::Main {
        fun call() {
            Ma/*caret*/
        }
    }    
    """)

    fun `test test_only modules not present in non test_only scopes`() = checkNoCompletion("""
    #[test_only]    
    module 0x1::TestHelpers {}
    module 0x1::Main {
        fun call() {
            TestH/*caret*/
        }
    }    
    """)
}
