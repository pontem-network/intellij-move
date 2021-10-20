package org.move.cli

import org.move.utils.TestProjectRootServiceImpl
import org.move.utils.rootService
import org.move.utils.tests.MoveTestBase
import org.move.utils.tests.base.TestCase
import java.nio.file.Paths

class DoveCommandTest : MoveTestBase() {

    fun `test fetch package metadata for a test project`() {
        val moveProjectRoot = Paths.get(TestCase.testResourcesPath).resolve("dove_toml_project")
        (project.rootService as TestProjectRootServiceImpl).modifyPath(moveProjectRoot)

        val debugMovePath = moveProjectRoot.resolve("stdlib").resolve("debug.move")
        val metadata = project.metadata(debugMovePath)

        check(metadata != null) { "Metadata is null" }
        check(metadata.dialect == "pont")

        val depFolders = metadata.depFolders
        check(depFolders.size == 2)
        check(depFolders[0].toNioPath().toString().endsWith("/artifacts/modules"))
        check(depFolders[1].toNioPath().toString().endsWith("intellij-move/src/test/resources/dove_toml_project/stdlib"))

        check(metadata.account_address == "0x1")
    }
}
