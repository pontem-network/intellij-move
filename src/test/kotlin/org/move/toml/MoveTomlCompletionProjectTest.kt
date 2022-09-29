package org.move.toml

import org.move.utils.tests.completion.CompletionProjectTestCase

class MoveTomlCompletionProjectTest : CompletionProjectTestCase() {
    fun `test local path completion in inline table`() = doSingleCompletion(
        {
            moveToml("""
[package]
name = "myname"

[dependencies]
MoveStdlib = { local = "./my_pa/*caret*/" }
            """)
            dir("my_path", {})
        },
        """
[package]
name = "myname"

[dependencies]
MoveStdlib = { local = "./my_path" }
        """
    )

    fun `test local path completion in dep table`() = doSingleCompletion(
        {
            moveToml("""
[package]
name = "myname"

[dependencies.MoveStdlib]
local = "./my_pa/*caret*/"
            """)
            dir("my_path", {})
        },
        """
[package]
name = "myname"

[dependencies.MoveStdlib]
local = "./my_path"
        """
    )
}
