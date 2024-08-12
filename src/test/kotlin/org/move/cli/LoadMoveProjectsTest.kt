package org.move.cli

import org.move.utils.tests.FileTreeBuilder
import org.move.utils.tests.MvProjectTestBase

class LoadMoveProjectsTest: MvProjectTestBase() {
    fun `test load project with invalid config yaml file`() {
        val moveProject = moveProject {
            _aptos {
                config_yaml(
                    """
---
profiles:
  default:
    "private_key": "0x1f9abf8196bd6b34731fdf99da384180ee43002befac0e55f39aceed9869e321",,,
                """
                )
            }
            sources { main("""<caret>""") }
            moveToml(
                """
[package]
name = "move_toml"
version = "0.1.0"
            """
            )
        }
        check(moveProject.currentPackage.packageName == "move_toml")
        check(moveProject.currentPackage.aptosConfigYaml == null)
    }

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
            _aptos {
                config_yaml(
                    """
---
profiles:
  default:
    private_key: "0x1f9abf8196bd6b34731fdf99da384180ee43002befac0e55f39aceed9869e321"
    public_key: "0x21dae149d5c16ec825558eb86c6434a2aa4bd1a54b66430dfdea983f3f5faaec"
    account: 2ec4190dd6eec80913e02da22de89700a9b5e13e27b51750191b7ceb3eee1a2f
    rest_url: "https://fullnode.testnet.aptoslabs.com"
  emergency:
    private_key: "0x3976a9fa9196a4e0240e64d1837fec879d65229194aef942fb81a7b41ff62912"
    public_key: "0xb1af70c600661e19d631296d89b8fd51aecafd2e7da76d27a9f462046647e17e"
    account: c5f39b983cf06b9e26dc149b3a8c0d7fcb27733954fa86eff7f3c70427644b1f
    rest_url: "https://fullnode.testnet.aptoslabs.com"
                """
                )
            }
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

        check(movePackage.aptosConfigYaml?.profiles == setOf("default", "emergency"))

        check(moveToml.packageTable?.name == "move_toml")
        check(moveToml.packageTable?.version == "0.1.0")
        check(moveToml.packageTable?.authors.orEmpty().isEmpty())
        check(moveToml.packageTable?.license == null)

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
