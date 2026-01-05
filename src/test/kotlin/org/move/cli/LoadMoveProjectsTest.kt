package org.move.cli

import org.move.utils.tests.FileTreeBuilder
import org.move.utils.tests.MvProjectTestBase

class LoadMoveProjectsTest: MvProjectTestBase() {
    fun `test load project invalid move toml file`() {
        moveProject {
            sources { main("""<caret>""") }
            moveToml(
                """
[package]]
name = "move_toml"
version = "0.1.0"
            """
            )
        }
    }

    fun `test load valid project`() {
        val moveProject = moveProject {
            sources { main("""<caret>""") }
            moveToml(
                """
[package]
name = "move_toml"
version = "0.1.0"

[addresses]
Std = "0x1"
DiemFramework = "0xB1E55ED"

[dependencies]
Debug = { local = "./stdlib/Debug.move" }
            """
            )
        }
        val movePackage = moveProject.currentPackage
        val moveToml = movePackage.moveToml

        check(moveToml.packageTable?.name == "move_toml")
        check(moveToml.packageTable.version == "0.1.0")
        check(moveToml.packageTable.authors.isEmpty())
        check(moveToml.packageTable.license == null)

        check(moveToml.addresses.size == 2)
        check(moveToml.addresses["Std"]!!.first == "0x1")
        check(moveToml.addresses["DiemFramework"]!!.first == "0xB1E55ED")

        check(moveToml.deps.size == 1)
    }

    private fun moveProject(builder: FileTreeBuilder.() -> Unit): MoveProject {
        val testProject = testProject(builder)
        val moveProject = testProject.project.moveProjectsService.allProjects.singleOrNull()
            ?: error("Move project expected")
        return moveProject
    }
}
