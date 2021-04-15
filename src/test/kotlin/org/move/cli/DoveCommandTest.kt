package org.move.cli

import com.intellij.ide.util.PropertiesComponent
import com.intellij.util.io.exists
import org.move.utils.tests.MoveTestBase
import org.move.utils.tests.base.TestCase
import java.nio.file.Paths

class DoveCommandTest : MoveTestBase() {

    override fun setUp() {
        super.setUp()
        val doveExecutablePath = Paths.get("dove")
        check(doveExecutablePath.exists()) { "$doveExecutablePath file does not exist" }

        PropertiesComponent.getInstance(this.project)
                .setValue(
                    Constants.DOVE_EXECUTABLE_PATH_PROPERTY,
                    doveExecutablePath.toAbsolutePath().toString()
                )
    }

    fun `test fetch package metadata for a test project`() {
        val moveProjectRoot = Paths.get(TestCase.testResourcesPath).resolve("move_project")
        val metadata = DoveExecutable(project).metadata(moveProjectRoot)!!

        check(metadata.package_info.dialect == "dfinance")
        check(
            metadata.package_info.local_dependencies == listOf(
                moveProjectRoot.resolve("stdlib").toAbsolutePath().toString()
            )
        )
        check(metadata.package_info.account_address == "0x0000000000000000000000000000000000000001")
    }
}
