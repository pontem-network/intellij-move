package org.move.ide.inspections

import org.move.ide.inspections.FieldInitShorthandInspection
import org.move.ide.inspections.lints.MvConstNamingInspection
import org.move.ide.inspections.lints.MvLocalBindingNamingInspection
import org.move.ide.inspections.lints.MvStructNamingInspection
import org.move.utils.tests.annotation.InspectionsTestCase

class MvConstNamingInspectionTest: InspectionsTestCase(MvConstNamingInspection::class) {
    fun `test constants`() = checkByText("""
module 0x1::M {
    const CONST_OK: u8 = 1;
    const <warning descr="Invalid constant name `const_foo`. Constant names must start with 'A'..'Z'">const_foo</warning>: u8 = 2;
}
    """)
}

class MvStructNamingInspectionTest: InspectionsTestCase(MvStructNamingInspection::class) {
    fun `test structs`() = checkByText("""
module 0x1::M {
    struct S {}
    struct <warning descr="Invalid struct name `collection`. Struct names must start with 'A'..'Z'">collection</warning> {}
}
    """)
}

class MvLocalBindingNamingInspectionTest: InspectionsTestCase(MvLocalBindingNamingInspection::class) {
    fun `test function parameter`() = checkByText("""
module 0x1::M {
    fun m(<warning descr="Invalid local variable name `COLL`. Local variable names must start with 'a'..'z'">COLL</warning>: u8) {}
}
    """)

    fun `test let variables`() = checkByText("""
module 0x1::M {
    fun m() {
        let <warning descr="Invalid local variable name `COLL`. Local variable names must start with 'a'..'z'">COLL</warning> = 1;
    }
}
    """)

    fun `test let variables inside pattern`() = checkByText("""
module 0x1::M {
    struct S { s: u8 }
    fun m() {
        let S { s: <warning descr="Invalid local variable name `COLL`. Local variable names must start with 'a'..'z'">COLL</warning> } = S { s: 1 };
    }
}
    """)

    fun `test let variable can be underscored`() = checkByText("""
module 0x1::M {
    fun m(_param: u8, _: u8) {
        let _ = 1;
        let _s = 2;
    }
}        
    """)
}
