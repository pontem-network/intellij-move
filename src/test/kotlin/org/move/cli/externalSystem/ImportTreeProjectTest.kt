package org.move.cli.externalSystem

import org.move.utils.tests.MvProjectTestBase

class ImportTreeProjectTest : MvProjectTestBase() {
    fun `test import project with circular dependencies no stackoverflow`() {
        testProject {
            moveToml(
                """
            [package]
            name = "MyPackage"
                
            [dependencies]
            MyLocal = { local = "." }
            """
            )
            sources { main("/*caret*/") }
        }
    }
}
