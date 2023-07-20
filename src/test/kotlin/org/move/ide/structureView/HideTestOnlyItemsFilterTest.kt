package org.move.ide.structureView

class HideTestOnlyItemsFilterTest: StructureViewToggleableActionTestBase() {
    fun `test hide test only items filter`() = doTest("""
        module 0x1::m {
            struct S {}
            #[test_only]
            struct ST {}
            
            fun main() {}
            #[test_only]
            fun main_t() {}
        }        
    """, """
-main.move
 -m
  S
  ST
  main()
  main_t()
    """, """
-main.move
 -m
  S
  main()
    """)

    override val actionId: String get() = HideTestOnlyItemsFilter.ID
}