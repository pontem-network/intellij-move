package org.move.ide.inspections

import org.intellij.lang.annotations.Language
import org.move.utils.tests.annotation.InspectionTestBase

class ReplaceWithMethodCallInspectionTest: InspectionTestBase(ReplaceWithMethodCallInspection::class) {

    fun `test no warning if first parameter is not self`() = doTest(
        """
        module 0x1::main {
            struct S { field: u8 }
            fun get_field(s: &S): u8 { s.field }
            fun main(s: S) {
                get_field(&s);
            }
        }        
    """
    )

    fun `test no warning if first parameter has different type`() = doTest(
        """
        module 0x1::main {
            struct S { field: u8 }
            struct T { field: u8 }
            fun get_field(self: &T): u8 { s.field }
            fun main(s: S) {
                get_field(&s);
            }
        }        
    """
    )

    fun `test no warning if references are incompatible`() = doTest(
        """
        module 0x1::main {
            struct S { field: u8 }
            fun get_field(s: &mut S): u8 { s.field }
            fun main(s: &S) {
                get_field(s);
            }
        }        
    """
    )

    fun `test no warning if self parameter struct is from another module`() = doTest(
        """
        module 0x1::m {
            struct S { field: u8 }
        }
        module 0x1::main {
            use 0x1::m::S;
            fun get_field(self: S): u8 { s.field }
            fun main(s: S) {
                get_field(s);
            }
        }        
    """
    )

    fun `test no warning if self parameter is not provided`() = doTest(
        """
        module 0x1::main {
            struct S { field: u8 }
            fun get_field(s: S): u8 { s.field }
            fun main(s: S) {
                get_field();
            }
        }        
    """
    )

    fun `test no warning if not enough parameters`() = doTest(
        """
        module 0x1::main {
            struct S { field: u8 }
            fun get_field(s: S, a: u8, b: u8): u8 { s.field }
            fun main(s: S) {
                get_field(s, 1);
            }
        }        
    """
    )

    fun `test no warning if generics are incompatible`() = doTest(
        """
        module 0x1::main {
            struct S<T> { field: T }
            fun get_field(self: &S<u8>): u8 { s.field }
            fun main(s: &S<u16>) {
                get_field(s);
            }
        }        
    """
    )

    fun `test no warning if generic is unknown`() = doTest(
        """
        module 0x1::main {
            struct S<T> { field: T }
            fun get_field(self: &S<u8>): u8 { s.field }
            fun main(s: &S<u24>) {
                get_field(s);
            }
        }        
    """
    )

    fun `test fix if method`() = doFixTest(
        """
        module 0x1::main {
            struct S { field: u8 }
            fun get_field(self: S): u8 { s.field }
            fun main(s: S) {
                <weak_warning descr="Can be replaced with method call">/*caret*/get_field(s)</weak_warning>;
            }
        }        
    """,
        """
        module 0x1::main {
            struct S { field: u8 }
            fun get_field(self: S): u8 { s.field }
            fun main(s: S) {
                s.get_field();
            }
        }        
    """,
    )

    fun `test fix if method with parameters`() = doFixTest(
        """
        module 0x1::main {
            struct S { field: u8 }
            fun get_field(self: S, a: u8, b: u8): u8 { s.field }
            fun main(s: S) {
                <weak_warning descr="Can be replaced with method call">/*caret*/get_field(s, 3, 4)</weak_warning>;
            }
        }        
    """,
        """
        module 0x1::main {
            struct S { field: u8 }
            fun get_field(self: S, a: u8, b: u8): u8 { s.field }
            fun main(s: S) {
                s.get_field(3, 4);
            }
        }        
    """,
    )

    fun `test fix if method of imported struct`() = doFixTest(
        """
        module 0x1::m {
            struct S { field: u8 }
            public fun get_field(self: S): u8 { s.field }
        }
        module 0x1::main {
            use 0x1::m::S;
            use 0x1::m::get_field;
            fun main(s: S) {
                <weak_warning descr="Can be replaced with method call">/*caret*/get_field(s)</weak_warning>;
            }
        }        
    """,
        """
        module 0x1::m {
            struct S { field: u8 }
            public fun get_field(self: S): u8 { s.field }
        }
        module 0x1::main {
            use 0x1::m::S;
            use 0x1::m::get_field;
            fun main(s: S) {
                s.get_field();
            }
        }        
    """,
    )

    fun `test fix if method and autoborrow`() = doFixTest(
        """
        module 0x1::main {
            struct S { field: u8 }
            fun get_field(self: &S): u8 { s.field }
            fun main(s: S) {
                <weak_warning descr="Can be replaced with method call">/*caret*/get_field(&s)</weak_warning>;
            }
        }        
    """,
        """
        module 0x1::main {
            struct S { field: u8 }
            fun get_field(self: &S): u8 { s.field }
            fun main(s: S) {
                s.get_field();
            }
        }        
    """,
    )

    fun `test fix if method and compatible reference`() = doFixTest(
        """
        module 0x1::main {
            struct S { field: u8 }
            fun get_field(self: &S): u8 { s.field }
            fun main(s: &mut S) {
                <weak_warning descr="Can be replaced with method call">/*caret*/get_field(s)</weak_warning>;
            }
        }        
    """,
        """
        module 0x1::main {
            struct S { field: u8 }
            fun get_field(self: &S): u8 { s.field }
            fun main(s: &mut S) {
                s.get_field();
            }
        }        
    """,
    )

    fun `test replace with method call tranfer type arguments`() = doFixTest("""
        module 0x1::main {
            struct S<T> { field: u8 }
            native fun get_type<U, T>(self: &S<U>): T;
            fun main<T>(s: S<T>) {
                <weak_warning descr="Can be replaced with method call">/*caret*/get_type<T, u8>(&s)</weak_warning>;
            }
        }        
    """, """
        module 0x1::main {
            struct S<T> { field: u8 }
            native fun get_type<U, T>(self: &S<U>): T;
            fun main<T>(s: S<T>) {
                s.get_type<T, u8>();
            }
        }        
    """)

    fun `test replace with deref expr`() = doFixTest("""
        module 0x1::main {
            struct String { bytes: vector<u8> }
            public native fun sub_string(self: &String, i: u64, j: u64): String;
            fun main(key: &String) {
                <weak_warning descr="Can be replaced with method call">/*caret*/sub_string(&*key, 1, 2)</weak_warning>;
            } 
        }        
    """, """
        module 0x1::main {
            struct String { bytes: vector<u8> }
            public native fun sub_string(self: &String, i: u64, j: u64): String;
            fun main(key: &String) {
                (*key).sub_string(1, 2);
            } 
        }        
    """)

    fun `test replace with copy expr`() = doFixTest("""
        module 0x1::main {
            struct String { bytes: vector<u8> }
            public native fun sub_string(self: &String, i: u64, j: u64): String;
            fun main(key: &String) {
                <weak_warning descr="Can be replaced with method call">/*caret*/sub_string(copy key, 1, 2)</weak_warning>;
            } 
        }        
    """, """
        module 0x1::main {
            struct String { bytes: vector<u8> }
            public native fun sub_string(self: &String, i: u64, j: u64): String;
            fun main(key: &String) {
                (copy key).sub_string(1, 2);
            } 
        }        
    """)

    private fun doTest(@Language("Move") text: String) =
        checkByText(text, checkWarn = false, checkWeakWarn = true)

    private fun doFixTest(
        @Language("Move") before: String,
        @Language("Move") after: String,
    ) =
        checkFixByText("Replace with method call", before, after,
                       checkWarn = false, checkWeakWarn = true)
}