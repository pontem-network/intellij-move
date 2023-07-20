package org.move.ide.structureView

class HidePrivateFunctionsFilterTest: StructureViewToggleableActionTestBase() {
    fun `test hide private functions filter`() = doTest("""
        module 0x1::m {
            public fun public_main() {}
            fun private_main() {}
        }        
    """, """
-main.move
 -m
  public_main()
  private_main()
    """, """
-main.move
 -m
  public_main()
    """)

    override val actionId: String get() = HidePrivateFunctionsFilter.ID
}