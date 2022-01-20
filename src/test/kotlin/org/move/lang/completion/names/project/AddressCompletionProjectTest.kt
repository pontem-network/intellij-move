package org.move.lang.completion.names.project

import org.move.utils.tests.completion.CompletionProjectTestCase

class AddressCompletionProjectTest: CompletionProjectTestCase() {
    fun `test use address completion`() = checkContainsCompletionsExact({
        moveToml("""
        [addresses]
        Std = "0x1"    
        Sender = "0xC0FFEE"    
        """)
        sources {
            move("main.move", """
            module 0x1::M {
                use S/*caret*/
            }    
            """)
        }
    }, listOf("Std", "Sender"))

    fun `test @ address completion`() = checkContainsCompletionsExact({
        moveToml("""
        [addresses]
        Std = "0x1"    
        Sender = "0xC0FFEE"    
        """)
        sources {
            move("main.move", """
            module 0x1::M {
                fun m() {
                    @S/*caret*/
                }
            }    
            """)
        }
    }, listOf("Std", "Sender"))

    fun `test autocomplete address before module in definition`() = doSingleCompletion(
        {
            moveToml(
                """
        [addresses]
        Sender = "0xC0FFEE"    
        """
            )
            sources {
                move(
                    "main.move", """
            module Se/*caret*/M {}
            """
                )
            }
        }, """
            module Sender::/*caret*/M {}
        """
    )

    fun `test autocomplete address before module in definition colon colon present`() = doSingleCompletion(
        {
            moveToml(
                """
        [addresses]
        Sender = "0xC0FFEE"    
        """
            )
            sources {
                move(
                    "main.move", """
            module Se/*caret*/::M {}
            """
                )
            }
        }, """
            module Sender::/*caret*/M {}
        """
    )

    fun `test module address completion`() = checkContainsCompletionsExact(
        {
            moveToml(
                """
        [addresses]
        Std = "0x1"    
        Sender = "0xC0FFEE"    
        """
            )
            sources {
                move(
                    "main.move", """
            module S/*caret*/M {}    
            """
                )
            }
        }, listOf("Std", "Sender")
    )
}
