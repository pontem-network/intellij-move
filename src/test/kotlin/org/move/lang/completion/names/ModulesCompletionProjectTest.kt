package org.move.lang.completion.names

import org.move.utils.tests.completion.CompletionProjectTestCase

class ModulesCompletionProjectTest : CompletionProjectTestCase() {

    fun `test complete modules from all the files`() = checkContainsCompletionsExact(
        """
        //- Move.toml
        //- sources/module.move
        module 0x1::M1 {}
        //- sources/main.move
        module 0x1::M2 {}
        script {
            use 0x1::/*caret*/
        }
    """, listOf("M1", "M2")
    )
}
