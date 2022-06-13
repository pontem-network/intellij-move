package org.move.cli

import org.move.cli.manifest.MoveToml
import org.move.openapiext.parseToml
import org.move.utils.TestProjectRootServiceImpl
import org.move.utils.rootService
import org.move.utils.tests.MvTestBase
import org.move.utils.tests.base.TestCase
import java.nio.file.Paths

class MoveTomlTest : MvTestBase() {
    fun `test parse move toml file`() {
        val moveProjectRoot = Paths.get(TestCase.testResourcesPath).resolve("move_toml_project")
        (project.rootService as TestProjectRootServiceImpl).modifyPath(moveProjectRoot)

        val manifestPath = moveProjectRoot.resolve(Consts.MANIFEST_FILE)
        val tomlFile = parseToml(project, manifestPath)!!

        val moveToml = MoveToml.fromTomlFile(tomlFile, moveProjectRoot)
        check(moveToml.packageTable?.name == "move_toml")
        check(moveToml.packageTable?.version == "0.1.0")
        check(moveToml.packageTable?.authors.orEmpty().isEmpty())
        check(moveToml.packageTable?.license == null)

        check(moveToml.addresses.size == 2)
        check(moveToml.addresses["Std"]!!.first == "0x1")
        check(moveToml.addresses["DiemFramework"]!!.first == "0xB1E55ED")

        check(moveToml.deps.size == 1)
//        check(
//            (moveToml.dependencies["Debug"]?.first as? Dependency.Local)?.absoluteLocalPath!!
//                .toString()
//                .endsWith("intellij-move/src/test/resources/move_toml_project/stdlib/Debug.move")
//        ) { (moveToml.dependencies["Debug"]?.first as? Dependency.Local)?.absoluteLocalPath!! }
    }
}
