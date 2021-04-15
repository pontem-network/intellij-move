package org.move.cli

import com.intellij.util.io.exists
import org.move.project.settings.moveSettings
import org.move.utils.tests.MoveTestBase
import java.nio.file.Paths

//class DoveCommandTest : MoveTestBase() {
//    override fun setUp() {
//        super.setUp()
//        val executable = Paths.get(".").toAbsolutePath().normalize().resolve("dove")
//        check(executable.exists()) { "No dove executable $executable" }
//        project.moveSettings
//            .modifyTemporary(testRootDisposable) { it.doveExecutable = executable.toString() }
//    }

//    fun `test fetch package metadata for a test project`() {
//        val cwd = Paths.get(".").toAbsolutePath().normalize()
//        val testProjDir = cwd.resolve("src/test/resources/org/move/dove/proj")
//
//        val pkg = Dove.fetchPackageMetadata(project, testProjDir)
//        check(pkg.account_address == "0000000000000000000000000000000000000001")
//        check(pkg.authors == emptyList<String>())
//        check(pkg.git_dependencies == listOf(DoveMetadata.GitDependency("https://github.com/dfinance/move-stdlib")))
//    }

//    fun `test fetch layout metadata for a test project`() {
//        val cwd = Paths.get(".").toAbsolutePath().normalize()
//        val testProjDir = cwd.resolve("src/test/resources/org/move/dove/proj")
//
//        val layout = Dove.fetchLayoutMetadata(project, testProjDir)
//        check(layout.module_dir == "modules")
//        check(layout.tests_dir == "tests")
//    }
//}
