package org.move.ide.annotator

class MvErrorAnnotatorTest : MvAnnotatorTestBase(MvErrorAnnotator::class) {
    fun `test duplicate function definition in script`() = checkError(
        """
        script {
            <error descr="Duplicate definitions with name `main`">fun main() {}</error>
            <error descr="Duplicate definitions with name `main`">fun main() {}</error>
        }
    """.trimIndent()
    )

    fun `test duplicate function definition in module`() = checkError(
        """
        module M {
            <error descr="Duplicate definitions with name `main`">fun main() {}</error>
            <error descr="Duplicate definitions with name `main`">fun main() {}</error>
        }
    """.trimIndent()
    )
//
//    fun `test duplicate module definition`() = checkError(
//        """
//        address {
//            <error descr="Duplicate definitions with name `M`">module M {}</error>
//            <error descr="Duplicate definitions with name `M`">module M {}</error>
//        }
//    """.trimIndent()
//    )
}