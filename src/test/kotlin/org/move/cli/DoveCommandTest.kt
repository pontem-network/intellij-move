package org.move.cli

import com.intellij.util.io.exists
import org.move.settings.moveSettings
import org.move.utils.TestProjectRootServiceImpl
import org.move.utils.rootService
import org.move.utils.tests.MoveTestBase
import org.move.utils.tests.base.TestCase
import java.nio.file.Paths

class DoveCommandTest : MoveTestBase() {

    fun `test fetch package metadata for a test project`() {
        val doveExecutablePath = Paths.get("dove")
        check(doveExecutablePath.exists()) { "$doveExecutablePath file does not exist" }

        project.moveSettings.modifyTemporary(testRootDisposable) {
            it.doveExecutablePath = doveExecutablePath.toAbsolutePath().toString()
        }

        val moveProjectRoot = Paths.get(TestCase.testResourcesPath).resolve("move_project")
        (project.rootService as TestProjectRootServiceImpl).modifyPath(moveProjectRoot)

        project.metadataService.refresh()

        val metadata = project.metadataService.metadata
        check(metadata != null) { "Metadata is null" }
        check(metadata.package_info.dialect == "dfinance")
        check(
            metadata.package_info.local_dependencies == listOf(
                moveProjectRoot.resolve("stdlib").toAbsolutePath().toString()
            )
        )
        check(metadata.package_info.account_address == "0x0000000000000000000000000000000000000001")
    }
}
