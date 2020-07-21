package org.move.ide

import org.move.ide.annotator.ErrorAnnotator
import org.move.lang.utils.tests.annotator.AnnotatorTestCase

class ErrorAnnotatorTest : AnnotatorTestCase(ErrorAnnotator::class) {
    fun `test duplicate function definition in script`() = checkError(
        """
        script {
            fun <error descr="Duplicate definitions with name `main`">main</error>() {}
            fun <error descr="Duplicate definitions with name `main`">main</error>() {}
        }
    """.trimIndent()
    )

    fun `test duplicate function definitions in module`() = checkError(
        """
        module M {
            fun <error descr="Duplicate definitions with name `main`">main</error>() {}
            fun <error descr="Duplicate definitions with name `main`">main</error>() {}
            native fun <error descr="Duplicate definitions with name `main`">main</error>();
        }
    """.trimIndent()
    )

    fun `test duplicate module definition`() = checkError(
        """
        address 0x0 {
            module <error descr="Duplicate definitions with name `MyModule`">MyModule</error> {}
            module <error descr="Duplicate definitions with name `MyModule`">MyModule</error> {}
        }
    """.trimIndent()
    )

    fun `test duplicate module definition at the toplevel`() = checkError(
        """
        module <error descr="Duplicate definitions with name `MyModule`">MyModule</error> {}
        module <error descr="Duplicate definitions with name `MyModule`">MyModule</error> {}
    """.trimIndent()
    )
}
