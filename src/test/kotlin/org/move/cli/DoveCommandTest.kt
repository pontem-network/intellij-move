package org.move.cli

import org.move.utils.TestProjectRootServiceImpl
import org.move.utils.rootService
import org.move.utils.tests.MoveTestBase
import org.move.utils.tests.base.TestCase
import java.nio.file.Paths

class DoveCommandTest : MoveTestBase() {

    fun `test fetch package metadata for a test project`() {
        val moveProjectRoot = Paths.get(TestCase.testResourcesPath).resolve("move_project")
        (project.rootService as TestProjectRootServiceImpl).modifyPath(moveProjectRoot)

        val debugMovePath = moveProjectRoot.resolve("stdlib").resolve("debug.move")
        val metadata = project.metadata(debugMovePath)

        check(metadata != null) { "Metadata is null" }
        check(metadata.packageTable?.dialect == "pont")

        val dependencies = metadata.packageTable?.dependencies.orEmpty()
        check(dependencies.size == 2)
        check(dependencies[0].toString().endsWith("/artifacts/modules"))
        check(dependencies[1].toString().endsWith("intellij-move/src/test/resources/move_project/stdlib"))

        check(metadata.packageTable?.account_address == "0x1")
    }
}
