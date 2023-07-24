package org.move.ide.structureView

import com.intellij.ide.structureView.newStructureView.StructureViewComponent
import com.intellij.openapi.ui.Queryable
import com.intellij.testFramework.PlatformTestUtil
import org.intellij.lang.annotations.Language
import org.move.utils.tests.MvTestBase
import javax.swing.JTree
import javax.swing.tree.TreePath

abstract class StructureViewTestBase: MvTestBase() {
    protected fun doTestStructureView(
        @Language("Move") code: String,
        actions: StructureViewComponent.() -> Unit
    ) {
        myFixture.configureByText("main.move", code)
        myFixture.testStructureView {
            it.actions()
        }
    }

    protected fun assertTreeEqual(tree: JTree, expected: String) {
        val printInfo = Queryable.PrintInfo(
            arrayOf(MvStructureViewTreeElement.NAME_KEY),
        )
        PlatformTestUtil.expandAll(tree)
        val treeStringPresentation = PlatformTestUtil.print(
            tree,
            TreePath(tree.model.root),
            printInfo,
            false
        )
        assertEquals(expected.trim(), treeStringPresentation.trim())
    }
}