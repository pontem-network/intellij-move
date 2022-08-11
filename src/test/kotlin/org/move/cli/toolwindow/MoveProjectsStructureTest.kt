package org.move.cli.toolwindow

import com.intellij.ide.util.treeView.AbstractTreeStructure
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.ProjectViewTestUtil
import org.move.cli.moveProjects
import org.move.utils.tests.MvProjectTestBase
import org.move.utils.tests.TreeBuilder

class MoveProjectsStructureTest : MvProjectTestBase() {
    fun `test move projects`() = doTest("""
    Root
     Project(MyNestedPackage)
      Entrypoints
      Modules
     Project(MyPackage)
      Entrypoints
      Modules
    """) {
        namedMoveToml("MyPackage")
        sources { main("/*caret*/") }
        dir("nested") {
            namedMoveToml("MyNestedPackage")
            sources { main("") }
        }
    }

    private fun doTest(expectedTreeStructure: String, builder: TreeBuilder) {
        testProject(builder)
        val structure = MoveProjectsTreeStructure(
            MoveProjectsTree(),
            testRootDisposable,
            project.moveProjects.allProjects.toList()
        )
        assertStructureEqual(structure, expectedTreeStructure.trimIndent() + "\n")
    }

    /**
     * Same as [ProjectViewTestUtil.assertStructureEqual], but uses [CargoSimpleNode.toTestString] instead of [CargoSimpleNode.toString].
     */
    private fun assertStructureEqual(structure: AbstractTreeStructure, expected: String) {
        ProjectViewTestUtil.checkGetParentConsistency(structure, structure.rootElement)
        val actual = PlatformTestUtil.print(structure, structure.rootElement) {
            if (it is MoveProjectsTreeStructure.MoveSimpleNode) it.toTestString() else it.toString()
        }
        assertEquals(expected, actual)
    }
}
