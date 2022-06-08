package org.move.ide.inspections

import org.move.utils.tests.annotation.InspectionTestBase

class MvUnusedVariableInspectionTest: InspectionTestBase(MvUnusedVariableInspection::class) {
    fun `test used function parameter`() = checkByText("""
    module 0x1::M {
        fun call(a: u8): u8 {
            a + 1
        } 
    }    
    """)

    fun `test used variable`() = checkByText("""
    module 0x1::M {
        fun call(): u8 {
            let a = 1;
            a + 1
        } 
    }    
    """)

    fun `test unused function parameter`() = checkFixByText("Rename to _a", """
    module 0x1::M {
        fun call(<warning descr="Unused function parameter">/*caret*/a</warning>: u8): u8 {
            1
        } 
    }    
    """, """
    module 0x1::M {
        fun call(_a: u8): u8 {
            1
        } 
    }    
    """)

    fun `test unused variable`() = checkFixByText("Rename to _a", """
    module 0x1::M {
        fun call(): u8 {
            let <warning descr="Unused variable">/*caret*/a</warning> = 1;
        } 
    }    
    """, """
    module 0x1::M {
        fun call(): u8 {
            let _a = 1;
        } 
    }    
    """)
}
