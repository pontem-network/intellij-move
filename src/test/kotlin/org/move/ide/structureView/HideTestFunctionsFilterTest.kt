package org.move.ide.structureView

class HideTestFunctionsFilterTest: StructureViewToggleableActionTestBase() {
    fun `test hide test functions filter`() = doTest("""
        module 0x1::m {
            fun main() {}
            #[test]
            fun test_main() {}
        }        
    """, """
-main.move
 -m
  main()
  test_main()
    """, """
-main.move
 -m
  main()
    """)

    override val actionId: String get() = HideTestFunctionsFilter.ID
}