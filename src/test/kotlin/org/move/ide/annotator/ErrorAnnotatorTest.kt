package org.move.ide.annotator

import org.move.utils.tests.annotation.AnnotatorTestCase

class ErrorAnnotatorTest : AnnotatorTestCase(ErrorAnnotator::class) {
    fun `test duplicate function definition in script`() = checkErrors(
        """
        script {
            fun <error descr="Duplicate definitions with name `main`">main</error>() {}
            fun <error descr="Duplicate definitions with name `main`">main</error>() {}
        }
    """.trimIndent()
    )

    fun `test duplicate function definitions in module`() = checkErrors(
        """
        module M {
            fun <error descr="Duplicate definitions with name `main`">main</error>() {}
            fun <error descr="Duplicate definitions with name `main`">main</error>() {}
            native fun <error descr="Duplicate definitions with name `main`">main</error>();
        }
    """.trimIndent()
    )

    fun `test duplicate module definition`() = checkErrors(
        """
        address 0x0 {
            module <error descr="Duplicate definitions with name `MyModule`">MyModule</error> {}
            module <error descr="Duplicate definitions with name `MyModule`">MyModule</error> {}
        }
    """.trimIndent()
    )

    fun `test duplicate module definition at the toplevel`() = checkErrors(
        """
        module <error descr="Duplicate definitions with name `MyModule`">MyModule</error> {}
        module <error descr="Duplicate definitions with name `MyModule`">MyModule</error> {}
    """.trimIndent()
    )
}
