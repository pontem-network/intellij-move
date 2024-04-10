package org.move.cli.toolwindow

import com.intellij.ide.util.treeView.AbstractTreeStructure
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.ProjectViewTestUtil
import org.move.cli.moveProjectsService
import org.move.cli.toolwindow.MoveProjectsTreeStructure.MoveSimpleNode
import org.move.utils.tests.MvProjectTestBase
import org.move.utils.tests.TreeBuilder

class MoveProjectsStructureTest: MvProjectTestBase() {
    fun `test move projects`() = doTest(
        """
Root
 Project(DepPackage)
  Dependencies
  Entrypoints
   Entrypoint(0x1::DepModule::my_init)
  Modules
   Module(0x1::DepModule)
 Project(MyPackage)
  Dependencies
   Package(DepPackage)
    Entrypoints
     Entrypoint(0x1::DepModule::my_init)
    Modules
     Module(0x1::DepModule)
  Entrypoints
   Entrypoint(0x1::M::init)
  Modules
   Module(0x1::M)
  Views
   View(0x1::M::get_coin_value)
    """
    ) {
        moveToml(
            """
        [package]
        name = "MyPackage"
        
        [dependencies]
        DepPackage = { local = "./dependency" }
        """
        )
        sources {
            main(
                """
            module 0x1::M {
                #[view]
                public fun get_coin_value(): u64 {}
                entry fun init() { /*caret*/ }
            }    
        """
            )
        }
        dir("dependency") {
            namedMoveToml("DepPackage")
            sources {
                move(
                    "DepModule.move", """
                module 0x1::DepModule {
                    entry fun my_init() {}
                }    
                """
                )
            }
        }
    }

    fun `test deeply nested package`() = doTest(
        """
Root
 Project(MyPackage)
  Dependencies
    """
    ) {
        dir("root1") {
            dir("root2") {
                dir("root3") {
                    namedMoveToml("MyPackage")
                    sources {
                        main("/*caret*/")
                    }
                }
            }
        }
    }

    private fun doTest(expectedTreeStructure: String, builder: TreeBuilder) {
        testProject(builder)
        val structure = MoveProjectsTreeStructure(
            MoveProjectsTree(),
            testRootDisposable,
            project.moveProjectsService.allProjects.toList()
        )
        assertStructureEqual(structure, expectedTreeStructure.trimIndent() + "\n")
    }

    /**
     * Same as [ProjectViewTestUtil.assertStructureEqual],
     * but uses [MoveSimpleNode.toTestString] instead of [MoveSimpleNode.toString].
     */
    private fun assertStructureEqual(structure: AbstractTreeStructure, expected: String) {
        ProjectViewTestUtil.checkGetParentConsistency(structure, structure.rootElement)
        val actual = PlatformTestUtil.print(structure, structure.rootElement) {
            if (it is MoveSimpleNode) it.toTestString() else it.toString()
        }
        assertEquals(expected, actual)
    }
}
