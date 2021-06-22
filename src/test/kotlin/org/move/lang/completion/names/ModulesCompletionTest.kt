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
}
