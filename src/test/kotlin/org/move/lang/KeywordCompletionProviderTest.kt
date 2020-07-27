package org.move.lang

import org.move.utils.tests.completion.CompletionTestCase

class KeywordCompletionProviderTest : CompletionTestCase() {
    fun `test top level completion`() = completionFixture.checkContainsCompletion(
        """/*caret*/""",
        listOf("address")
    )

    fun `test no structs on top level`() = completionFixture.checkNotContainsCompletion(
        """str/*caret*/""",
        "struct"
    )

    fun `test address completion`() = completionFixture.checkContainsCompletion(
        """ address 0x0 { /*caret*/ }""",
        "module"
    )

    fun `test only module keyword in address`() = completionFixture.checkNotContainsCompletion(
        """ address 0x0 { /*caret*/ }""",
        listOf("address", "script", "let", "fun")
    )

    fun `test top level module declarations`() = completionFixture.checkContainsCompletion(
        """module M { /*caret*/ }""",
        listOf("resource", "struct", "public", "fun", "const", "use", "native")
    )

    fun `test top level module does not have 'as'`() = completionFixture.checkNotContainsCompletion(
        """module M { /*caret*/ }""",
        "as"
    )

    fun `test no completion in module name`() = completionFixture.checkNoCompletion(
        " module /*caret*/ {} "
    )

    fun `test no completion in address literal`() = completionFixture.checkNoCompletion(
        " address /*caret*/ {} "
    )
}