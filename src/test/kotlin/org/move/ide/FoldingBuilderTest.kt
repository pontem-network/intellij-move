package org.move.ide

import org.move.utils.tests.MoveTestBase

class FoldingBuilderTest: MoveTestBase() {
    override val dataPath = "org/move/ide/folding.fixtures"

    fun `test empty script`() = doTest()

    private fun doTest() {
        myFixture.testFolding("$testDataPath/$fileName")
    }
}
