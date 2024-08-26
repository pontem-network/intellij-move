package org.move.lang

import org.move.cli.moveProjectsService
import org.move.lang.core.psi.MvNamedAddress
import org.move.utils.tests.FileTreeBuilder
import org.move.utils.tests.MvProjectTestBase
import org.move.utils.tests.base.findElementAndDataInEditor

class NamedModulePathValuesTest: MvProjectTestBase() {
    fun `test named address`() = checkByFileTree {
        moveToml(
            """
        [addresses]
        Std = "0x1"
        """
        )
        sources {
            move(
                "main.move", """
            module Std::Module {}
                   //^ Std = 0x1
            """
            )
        }
    }

    fun `test placeholder address`() = checkByFileTree {
        moveToml(
            """
        [addresses]
        Std = "_"
        """
        )
        sources {
            move(
                "main.move", """
            module Std::Module {}
                   //^ Std = _
            """
            )
        }
    }

    fun `test dependency address`() = checkByFileTree {
        moveToml(
            """
        [dependencies]
        Stdlib = { local = "./stdlib" }
        """
        )
        dir("stdlib") {
            moveToml(
                """
            [addresses]
            Std = "0x1"
            """
            )
        }
        sources {
            move(
                "main.move", """
            module Std::Module {}
                   //^ Std = 0x1
            """
            )
        }
    }

    fun `test subst address value`() = checkByFileTree {
        moveToml(
            """
    [package]
    name = "rmrk"
    version = "0.0.0"

    [dependencies]
    Stdlib = { local = "./stdlib", addr_subst = { "Std" = "0xC0FFEE" }}
        """
        )
        sources {
            move(
                "main.move", """
        module Std::Module {}
             //^ Std = 0xC0FFEE
            """
            )
        }
        dir("stdlib") {
            moveToml(
                """
        [package]
        name = "Stdlib"
        version = "0.0.0"
        [addresses]
        Std = "_"
            """
            )
        }
    }

    fun `test cannot subst non placeholder`() = checkByFileTree {
        moveToml(
            """
    [package]
    name = "rmrk"
    version = "0.0.0"

    [dependencies]
    Stdlib = { local = "./stdlib", addr_subst = { "Std" = "0xC0FFEE" }}
        """
        )
        sources {
            move(
                "main.move", """
        module Std::Module {}
             //^ Std = 0x1
            """
            )
        }
        dir("stdlib") {
            moveToml(
                """
        [package]
        name = "Stdlib"
        version = "0.0.0"
        [addresses]
        Std = "0x1"
            """
            )
        }
    }

    fun `test named address of transitive dependency with subst`() = checkByFileTree {
//        buildInfo("Module", mapOf("Std" to "0002"))
        moveToml(
            """
        [package]
        name = "Module"
        [dependencies]
        PontStdlib = { local = "./pont-stdlib", addr_subst = { "Std" = "0x2" }}
        """
        )
        sources {
            move(
                "main.move", """
            module Std::M {
                  //^ Std = 0x2
            }
            """
            )
        }
        dir("pont-stdlib") {
            moveToml(
                """
            [dependencies]
            MoveStdlib = { local = "./move-stdlib" }
            """
            )
            dir("move-stdlib") {
                moveToml(
                    """
                [addresses]
                Std = "_"
                """
                )
            }
        }
    }

    fun `test named address from git dependency`() = checkByFileTree {
        dotMove {
            git("https://github.com/pontem-network/move-stdlib.git", "main") {
                moveToml(
                    """
                [package]
                name = "Stdlib"
                
                [addresses]
                Std = "0x2"    
                """
                )
            }
        }
        moveToml(
            """
    [package]
    name = "Module"
            
    [dependencies]
    Stdlib = { git = "https://github.com/pontem-network/move-stdlib.git", rev = "main"}          
        """
        )
        sources {
            move(
                "main.move", """
            module Std::Module {}
                  //^ Std = 0x2
            """
            )
        }
    }

    private fun checkByFileTree(
        fileTree: FileTreeBuilder.() -> Unit,
    ) {
        testProject(fileTree)

        val (address, data) = myFixture.findElementAndDataInEditor<MvNamedAddress>()
        val expectedValue = data.trim()

        val moveProject = project.moveProjectsService.findMoveProjectForPsiElement(address)!!
        val actualValue = moveProject.getNamedAddressTestAware(address.referenceName)!!.text()

        check(actualValue == expectedValue) {
            "Value mismatch. Expected $expectedValue, found: $actualValue"
        }
    }
}
