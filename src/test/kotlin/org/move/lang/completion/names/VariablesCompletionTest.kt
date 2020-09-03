package org.move.lang.completion.names

import org.move.utils.tests.completion.CompletionTestCase

class VariablesCompletionTest: CompletionTestCase() {
    fun `test local variable`() = doSingleCompletion("""
        script { 
            fun main(quux: u8) { 
                qu/*caret*/ 
            }
        }
    """, """
        script { 
            fun main(quux: u8) { 
                quux/*caret*/ 
            }
        }
    """)

    fun `test shadowing`() = doSingleCompletion("""
        script {
            fun main() {
                let foobar = b"foobar";
                let foobar = foobar + b"1";
                foo/*caret*/
            }
        }
    """, """
        script {
            fun main() {
                let foobar = b"foobar";
                let foobar = foobar + b"1";
                foobar/*caret*/
            }
        }
    """)

    fun `test shadowing in nested blocks`() = doSingleCompletion("""
        script {
            fun main() {
                let foobar = b"foobar";
                {
                    let foobar = b"foobar2";
                    foo/*caret*/;
                }
            }
        }
    """, """
        script {
            fun main() {
                let foobar = b"foobar";
                {
                    let foobar = b"foobar2";
                    foobar/*caret*/;
                }
            }
        }
    """)

    fun `test local scope`() = checkNoCompletion("""
        script {
            fun main() {
                let x = spam/*caret*/;
                let spamlot = 92;
            }
        }
    """)

    fun `test inside scope is unreachable`() = checkNoCompletion("""
        script {
            fun main() {
                {
                    let spamlot = 92;    
                };
                let x = spam/*caret*/;
            }
        }
    """)
}