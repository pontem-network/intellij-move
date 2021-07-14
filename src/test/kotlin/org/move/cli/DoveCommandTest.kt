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

        project.metadataService.refresh()

        val metadata = project.metadataService.metadata
        check(metadata != null) { "Metadata is null" }
        check(metadata.packageTable?.dialect == "pont")
        println(metadata.packageTable?.dependencies)

        check(metadata.packageTable?.dependencies.orEmpty().size == 1)
        check(
            metadata
                .packageTable?.dependencies
                .orEmpty()[0].endsWith("intellij-move/src/test/resources/move_project/stdlib")
        )
        check(metadata.packageTable?.account_address == "0x1")
    }
}
