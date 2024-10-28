package org.move.lang.completion.names

import org.move.utils.tests.completion.CompletionTestCase

class LoopCompletionTest: CompletionTestCase() {
    fun `test no break keyword outside loop`() = checkNoCompletion("""
        module 0x1::m {
            fun main() {
                bre/*caret*/
            }
        }        
    """)

    fun `test break keyword inside loop`() = doSingleCompletion("""
        module 0x1::m {
            fun main() {
                loop {
                    bre/*caret*/
                }
            }
        }        
    """, """
        module 0x1::m {
            fun main() {
                loop {
                    break /*caret*/
                }
            }
        }        
    """)

    fun `test complete loop label for break`() = doSingleCompletion("""
        module 0x1::m {
            fun main() {
                'label: loop {
                    break 'la/*caret*/;
                }
            }
        }        
    """, """
        module 0x1::m {
            fun main() {
                'label: loop {
                    break 'label/*caret*/;
                }
            }
        }        
    """)

    fun `test complete loop label for break from single quote`() = doSingleCompletion("""
        module 0x1::m {
            fun main() {
                'label: loop {
                    break '/*caret*/;
                }
            }
        }        
    """, """
        module 0x1::m {
            fun main() {
                'label: loop {
                    break 'label/*caret*/;
                }
            }
        }        
    """)
}