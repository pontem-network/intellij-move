package org.move.ide.navigation

import com.intellij.openapi.ui.Queryable
import com.intellij.testFramework.PlatformTestUtil
import org.intellij.lang.annotations.Language
import org.move.utils.tests.MvTestBase
import javax.swing.JTree
import javax.swing.tree.TreePath

class StructureViewTest : MvTestBase() {
    fun `test address`() = doTest(
        """
address 0x1 {
    module M {
        fun call() {}
    }
}
    """, """
    -main.move
     -M
      call()
    """
    )

    fun `test module functions`() = doTest(
        """
    module M {
        public fun call_pub() {}
        public(friend) fun call_pub_friend() {}
        public(script) fun call_pub_script() {}
        fun double(i: u8): u8 {}
        fun main() {}
    }    
    """, """
    -main.move
     -M
      call_pub()
      call_pub_friend()
      call_pub_script()
      double(i: u8): u8
      main()
    """
    )

    fun `test scripts`() = doTest(
        """
    script {
        fun script_fun_1() {}
    }
    script {
        fun script_fun_2() {}
    }
    """, """
    -main.move
     script_fun_1()
     script_fun_2()
    """
    )

    private fun doTest(
        @Language("Move") code: String,
        expected: String,
        fileName: String = "main.move"
    ) {
        val normExpected = expected.trimIndent()
        myFixture.configureByText(fileName, code)
        myFixture.testStructureView {
            PlatformTestUtil.expandAll(it.tree)
            assertTreeEqual(it.tree, normExpected)
        }
    }

    private fun assertTreeEqual(tree: JTree, expected: String) {
        val printInfo = Queryable.PrintInfo(
            arrayOf(MvStructureViewElement.NAME_KEY),
        )
        val treeStringPresentation = PlatformTestUtil.print(tree, TreePath(tree.model.root), printInfo, false)
        assertEquals(expected.trim(), treeStringPresentation.trim())
    }
}
