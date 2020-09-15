package org.move.lang.completion

import org.intellij.lang.annotations.Language
import org.move.ide.annotator.BUILTIN_TYPE_IDENTIFIERS
import org.move.ide.annotator.PRIMITIVE_TYPE_IDENTIFIERS
import org.move.utils.tests.completion.CompletionTestCase

class PrimitiveTypesCompletionTest: CompletionTestCase() {
    fun `test builtin types present in type positions for function param`() = doTest("""
        script {
            fun main(signer: &/*caret*/) {
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
            fun main() { let a: vector</*caret*/>; }
        }
    """)

    fun `test no builtin types in expression position`() = checkNoCompletion("""
        script {
            fun main() {
                let a = /*caret*/
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

    private fun doTest(@Language("Move") text: String) {
        val primitiveTypeNames = PRIMITIVE_TYPE_IDENTIFIERS + BUILTIN_TYPE_IDENTIFIERS
        for (typeName in primitiveTypeNames) {
            checkContainsCompletion(typeName, text)
        }
    }
}