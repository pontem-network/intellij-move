package org.move.ide.actions

import com.intellij.ide.fileTemplates.FileTemplateManager
import org.intellij.lang.annotations.Language
import org.move.openapiext.toPsiDirectory
import org.move.utils.tests.MvProjectTestBase

class CreateMoveFileActionTest : MvProjectTestBase() {
    fun `test create module from template`() {
        testProject {
            moveToml(
                """
            [package]
            name = "MyPackage"
            
            [addresses]
            Sender = "0x1122"        
            """
            )
            sources { move("main.move", "/*caret*/") }
        }

        checkTextEquals(
            "Move Module", "MyModule", """
                module Sender::MyModule {}
            """
        )
    }

    fun `test create test module from template`() {
        testProject {
            moveToml(
                """
            [package]
            name = "MyPackage"
            
            [addresses]
            Sender = "0x1122"        
            """
            )
            sources { move("main.move", "/*caret*/") }
        }

        checkTextEquals(
            "Move Test Module", "MyModuleTests", """
                #[test_only]
                module Sender::MyModuleTests {}
            """
        )
    }

    private fun checkTextEquals(
        templateName: String,
        moduleName: String,
        @Language("Move") expectedText: String
    ) {
        val dir = this.rootDirectory
            ?.findChild("sources")?.toPsiDirectory(this.project)!!

        val template = FileTemplateManager.getInstance(this.project).getInternalTemplate(templateName)
        val resultFile = CreateMoveFileAction().createFileFromTemplate(dir, moduleName, template)!!

        val actual = resultFile.text.trimIndent()
        val expected = expectedText.trimIndent()
        check(actual == expected) {
            "Generated text\n=========\n${actual}\n=========\n\n is not equal " +
                    "to the expected\n\n=========\n${expected}\n=========\n"
        }
    }
}
