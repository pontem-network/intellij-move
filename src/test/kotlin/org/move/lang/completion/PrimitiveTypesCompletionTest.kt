package org.move.lang.completion

import org.intellij.lang.annotations.Language
import org.move.ide.annotator.BUILTIN_TYPE_IDENTIFIERS
import org.move.ide.annotator.PRIMITIVE_TYPE_IDENTIFIERS
import org.move.utils.tests.completion.CompletionTestCase

class PrimitiveTypesCompletionTest: CompletionTestCase() {
    fun `test builtin types present in type positions for function param`() = doTest("""
        script {
            fun main(val: /*caret*/) {
            }
        }
    """)

    fun `test builtin types present in borrow type positions for function param`() = doTest("""
        script {
            fun main(s: &/*caret*/) {
            }
        }
    """)

    fun `test builtin types present in borrow type positions for nested ref type`() = doTest("""
        script {
            fun main(s: &&/*caret*/) {
            }
        }
    """)

    fun `test builtin types present in type positions for let binding`() = doTest("""
        script {
            fun main() {
             let a: /*caret*/
            }
        }
    """)

    fun `test builtin types present in type positions for struct fields`() = doTest("""
        module M {
            struct MyStruct { val: /*caret*/ }
        }
    """)

    fun `test type in type parameter place`() = doTest("""
        module M {
            struct MyStruct<T> {}
            fun main() { let a: MyStruct</*caret*/>; }
        }
    """)

    fun `test no builtin types in expression position`() = checkNoCompletion("""
        script {
            fun main() {
                let a = u6/*caret*/
            }
        }
    """)

    fun `test no builtin types if module specified`() = checkNoCompletion("""
        script {
            fun main() {
                let a: Transaction::/*caret*/;
            }
        }
    """)

    fun `test vector completion`() = doSingleCompletion("""
        script {
            fun main(s: vec/*caret*/) {}
        }
    """, """
        script {
            fun main(s: vector</*caret*/>) {}
        }
    """)

    fun `test vector completion with angle brackets`() = doSingleCompletion("""
        script {
            fun main(s: vec/*caret*/<>) {}
        }
    """, """
        script {
            fun main(s: vector</*caret*/>) {}
        }
    """)

    private fun doTest(@Language("Move") text: String) {
        val typeNames = PRIMITIVE_TYPE_IDENTIFIERS + BUILTIN_TYPE_IDENTIFIERS
        for (typeName in typeNames) {
            checkContainsCompletion(typeName, text)
        }
    }
}