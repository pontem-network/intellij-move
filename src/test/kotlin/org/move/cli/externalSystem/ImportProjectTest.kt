package org.move.cli.projectAware

import org.move.utils.tests.MvProjectTestBase

class ImportProjectTest : MvProjectTestBase() {
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
