package org.move.cli

import org.move.cli.manifest.MoveToml
import org.move.openapiext.toPsiFile
import org.move.openapiext.toVirtualFile
import org.move.utils.TestProjectRootServiceImpl
import org.move.utils.rootService
import org.move.utils.tests.MvTestBase
import org.move.utils.tests.base.TestCase
import org.toml.lang.psi.TomlFile
import java.nio.file.Paths

class MoveTomlTest : MvTestBase() {
    fun `test parse move package`() {
//        val moveProjectRoot = Paths.get(TestCase.testResourcesPath).resolve("move_toml_project")
//        (project.rootService as TestProjectRootServiceImpl).modifyPath(moveProjectRoot)

//        val manifestPath = moveProjectRoot.resolve(Consts.MANIFEST_FILE)
//        val tomlFile = manifestPath.toVirtualFile()?.toPsiFile(project) as TomlFile
//
//        val moveToml = MoveToml.fromTomlFile(tomlFile, moveProjectRoot)
//        check(moveToml.packageTable?.name == "move_toml")
//        check(moveToml.packageTable?.version == "0.1.0")
//        check(moveToml.packageTable?.authors.orEmpty().isEmpty())
//        check(moveToml.packageTable?.license == null)
//
//        check(moveToml.addresses.size == 2)
//        check(moveToml.addresses["Std"]!!.first == "0x1")
//        check(moveToml.addresses["DiemFramework"]!!.first == "0xB1E55ED")
//
//        check(moveToml.deps.size == 1)
//
//        val movePackage = MovePackage.fromMoveToml(moveToml)!!
//        check(movePackage.aptosConfigYaml?.profiles == setOf("default", "emergency"))
    }
}
