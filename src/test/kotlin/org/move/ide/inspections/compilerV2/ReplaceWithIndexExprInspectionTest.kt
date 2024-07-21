package org.move.ide.inspections.compilerV2

import org.intellij.lang.annotations.Language
import org.move.utils.tests.annotation.InspectionTestBase

class ReplaceWithIndexExprInspectionTest: InspectionTestBase(ReplaceWithIndexExprInspection::class) {

    fun `test no error if types of arguments are incorrect`() = doTest("""
        module 0x1::vector {
            native public fun borrow<Element>(v: &vector<Element>, i: u64): &Element;
            native public fun borrow_mut<Element>(v: &mut vector<Element>, i: u64): &mut Element;
        }                
        module 0x1::m {
            use 0x1::vector;
            
            fun main() {
                let v = vector[1, 2];
                
                vector::borrow(0, &v);                
                vector::borrow(v, 0);                
                               
                vector::borrow_mut(0, &mut v);                
                vector::borrow_mut(v, 0);                
                vector::borrow_mut(&v, 0);                
            }            
        }
    """)

    fun `test no error if address is 0x2`() = doTest("""
        module 0x2::vector {
            native public fun borrow<Element>(v: &vector<Element>, i: u64): &Element;
            native public fun borrow_mut<Element>(v: &mut vector<Element>, i: u64): &mut Element;
        }                
        module 0x1::m {
            use 0x1::vector;
            
            fun main() {
                let v = vector[1, 2];
                vector::borrow(&v, 0);
                vector::borrow_mut(&mut v, 0);
            }            
        }
    """)

    fun `test no error if borrow deref is called but item is not copy`() = doTest("""
        module 0x1::vector {
            native public fun borrow<Element>(v: &vector<Element>, i: u64): &Element;
            native public fun borrow_mut<Element>(v: &mut vector<Element>, i: u64): &mut Element;
        }                
        module 0x1::m {
            use 0x1::vector;
            
            struct S { field: u8 }
            
            fun main() {
                let v = vector[S { field: 10 }];
                *vector::borrow(&v, 0);
            }            
        }
    """)

    fun `test replace vector borrow deref`() = doFixTest("""
        module 0x1::vector {
            native public fun borrow<Element>(v: &vector<Element>, i: u64): &Element;
            native public fun borrow_mut<Element>(v: &mut vector<Element>, i: u64): &mut Element;
        }                
        module 0x1::m {
            use 0x1::vector;
            
            fun main() {
                let v = vector[1, 2];
                let vv = &v;
                <weak_warning descr="Can be replaced with index expr">/*caret*/*vector::borrow(vv, 0)</weak_warning>;
            }            
        }
    """, """
        module 0x1::vector {
            native public fun borrow<Element>(v: &vector<Element>, i: u64): &Element;
            native public fun borrow_mut<Element>(v: &mut vector<Element>, i: u64): &mut Element;
        }                
        module 0x1::m {
            use 0x1::vector;
            
            fun main() {
                let v = vector[1, 2];
                let vv = &v;
                vv[0];
            }            
        }
    """)

    fun `test replace vector borrow deref with reference`() = doFixTest("""
        module 0x1::vector {
            native public fun borrow<Element>(v: &vector<Element>, i: u64): &Element;
            native public fun borrow_mut<Element>(v: &mut vector<Element>, i: u64): &mut Element;
        }                
        module 0x1::m {
            use 0x1::vector;
            
            fun main() {
                let v = vector[1, 2];
                <weak_warning descr="Can be replaced with index expr">/*caret*/*vector::borrow(&v, 0)</weak_warning>;
            }            
        }
    """, """
        module 0x1::vector {
            native public fun borrow<Element>(v: &vector<Element>, i: u64): &Element;
            native public fun borrow_mut<Element>(v: &mut vector<Element>, i: u64): &mut Element;
        }                
        module 0x1::m {
            use 0x1::vector;
            
            fun main() {
                let v = vector[1, 2];
                v[0];
            }            
        }
    """)

    fun `test replace vector borrow deref with mut reference`() = doFixTest("""
        module 0x1::vector {
            native public fun borrow<Element>(v: &vector<Element>, i: u64): &Element;
            native public fun borrow_mut<Element>(v: &mut vector<Element>, i: u64): &mut Element;
        }                
        module 0x1::m {
            use 0x1::vector;
            
            fun main() {
                let v = vector[1, 2];
                <weak_warning descr="Can be replaced with index expr">/*caret*/*vector::borrow(&mut v, 0)</weak_warning>;
            }            
        }
    """, """
        module 0x1::vector {
            native public fun borrow<Element>(v: &vector<Element>, i: u64): &Element;
            native public fun borrow_mut<Element>(v: &mut vector<Element>, i: u64): &mut Element;
        }                
        module 0x1::m {
            use 0x1::vector;
            
            fun main() {
                let v = vector[1, 2];
                v[0];
            }            
        }
    """)

    fun `test replace vector borrow deref with dot expr`() = doFixTest("""
        module 0x1::vector {
            native public fun borrow<Element>(v: &vector<Element>, i: u64): &Element;
            native public fun borrow_mut<Element>(v: &mut vector<Element>, i: u64): &mut Element;
        }                
        module 0x1::m {
            use 0x1::vector;
            
            struct S has copy { field: u8 }
            
            fun main() {
                let v = vector[S { field: 0 }];
                (<weak_warning descr="Can be replaced with index expr">/*caret*/*vector::borrow(&v, 0)</weak_warning>).field;
            }            
        }
    """, """
        module 0x1::vector {
            native public fun borrow<Element>(v: &vector<Element>, i: u64): &Element;
            native public fun borrow_mut<Element>(v: &mut vector<Element>, i: u64): &mut Element;
        }                
        module 0x1::m {
            use 0x1::vector;
            
            struct S has copy { field: u8 }
            
            fun main() {
                let v = vector[S { field: 0 }];
                (v[0]).field;
            }            
        }
    """)

//    fun `test replace vector borrow`() = doFixTest("""
//        module 0x1::vector {
//            native public fun borrow<Element>(v: &vector<Element>, i: u64): &Element;
//            native public fun borrow_mut<Element>(v: &mut vector<Element>, i: u64): &mut Element;
//        }
//        module 0x1::m {
//            use 0x1::vector;
//
//            fun main() {
//                let v = vector[1, 2];
//                <weak_warning descr="Can be replaced with index expr">/*caret*/vector::borrow(&v, 0)</weak_warning>;
//            }
//        }
//    """, """
//        module 0x1::vector {
//            native public fun borrow<Element>(v: &vector<Element>, i: u64): &Element;
//            native public fun borrow_mut<Element>(v: &mut vector<Element>, i: u64): &mut Element;
//        }
//        module 0x1::m {
//            use 0x1::vector;
//
//            fun main() {
//                let v = vector[1, 2];
//                &v[0];
//            }
//        }
//    """)

//    fun `test replace vector borrow with variable`() = doFixTest("""
//        module 0x1::vector {
//            native public fun borrow<Element>(v: &vector<Element>, i: u64): &Element;
//            native public fun borrow_mut<Element>(v: &mut vector<Element>, i: u64): &mut Element;
//        }
//        module 0x1::m {
//            use 0x1::vector;
//
//            fun main() {
//                let v = vector[1, 2];
//                let vv = &v;
//                <weak_warning descr="Can be replaced with index expr">/*caret*/vector::borrow(vv, 0)</weak_warning>;
//            }
//        }
//    """, """
//        module 0x1::vector {
//            native public fun borrow<Element>(v: &vector<Element>, i: u64): &Element;
//            native public fun borrow_mut<Element>(v: &mut vector<Element>, i: u64): &mut Element;
//        }
//        module 0x1::m {
//            use 0x1::vector;
//
//            fun main() {
//                let v = vector[1, 2];
//                let vv = &v;
//                vv[0];
//            }
//        }
//    """)

//    fun `test replace vector borrow mut`() = doFixTest("""
//        module 0x1::vector {
//            native public fun borrow<Element>(v: &vector<Element>, i: u64): &Element;
//            native public fun borrow_mut<Element>(v: &mut vector<Element>, i: u64): &mut Element;
//        }
//        module 0x1::m {
//            use 0x1::vector;
//
//            fun main() {
//                let v = vector[1, 2];
//                <weak_warning descr="Can be replaced with index expr">/*caret*/vector::borrow_mut(&v, 0)</weak_warning>;
//            }
//        }
//    """, """
//        module 0x1::vector {
//            native public fun borrow<Element>(v: &vector<Element>, i: u64): &Element;
//            native public fun borrow_mut<Element>(v: &mut vector<Element>, i: u64): &mut Element;
//        }
//        module 0x1::m {
//            use 0x1::vector;
//
//            fun main() {
//                let v = vector[1, 2];
//                &mut v[0];
//            }
//        }
//    """)

//    fun `test replace vector borrow with dot expr`() = doFixTest("""
//        module 0x1::vector {
//            native public fun borrow<Element>(v: &vector<Element>, i: u64): &Element;
//            native public fun borrow_mut<Element>(v: &mut vector<Element>, i: u64): &mut Element;
//        }
//        module 0x1::m {
//            use 0x1::vector;
//
//            struct S { field: u8 }
//
//            fun main() {
//                let v = vector[S { field: 0 }];
//                <weak_warning descr="Can be replaced with index expr">/*caret*/vector::borrow(&v, 0)</weak_warning>.field;
//            }
//        }
//    """, """
//        module 0x1::vector {
//            native public fun borrow<Element>(v: &vector<Element>, i: u64): &Element;
//            native public fun borrow_mut<Element>(v: &mut vector<Element>, i: u64): &mut Element;
//        }
//        module 0x1::m {
//            use 0x1::vector;
//
//            struct S { field: u8 }
//
//            fun main() {
//                let v = vector[S { field: 0 }];
//                (&v[0]).field;
//            }
//        }
//    """)

//    fun `test replace vector borrow with dot expr with variable`() = doFixTest("""
//        module 0x1::vector {
//            native public fun borrow<Element>(v: &vector<Element>, i: u64): &Element;
//            native public fun borrow_mut<Element>(v: &mut vector<Element>, i: u64): &mut Element;
//        }
//        module 0x1::m {
//            use 0x1::vector;
//
//            struct S { field: u8 }
//
//            fun main() {
//                let v = vector[S { field: 0 }];
//                let vv = &v;
//                <weak_warning descr="Can be replaced with index expr">/*caret*/vector::borrow(vv, 0)</weak_warning>.field;
//            }
//        }
//    """, """
//        module 0x1::vector {
//            native public fun borrow<Element>(v: &vector<Element>, i: u64): &Element;
//            native public fun borrow_mut<Element>(v: &mut vector<Element>, i: u64): &mut Element;
//        }
//        module 0x1::m {
//            use 0x1::vector;
//
//            struct S { field: u8 }
//
//            fun main() {
//                let v = vector[S { field: 0 }];
//                let vv = &v;
//                vv[0].field;
//            }
//        }
//    """)

//    fun `test replace vector borrow mut with dot expr`() = doFixTest("""
//        module 0x1::vector {
//            native public fun borrow<Element>(v: &vector<Element>, i: u64): &Element;
//            native public fun borrow_mut<Element>(v: &mut vector<Element>, i: u64): &mut Element;
//        }
//        module 0x1::m {
//            use 0x1::vector;
//
//            struct S { field: u8 }
//
//            fun main() {
//                let v = vector[S { field: 0 }];
//                <weak_warning descr="Can be replaced with index expr">/*caret*/vector::borrow_mut(&mut v, 0)</weak_warning>.field;
//            }
//        }
//    """, """
//        module 0x1::vector {
//            native public fun borrow<Element>(v: &vector<Element>, i: u64): &Element;
//            native public fun borrow_mut<Element>(v: &mut vector<Element>, i: u64): &mut Element;
//        }
//        module 0x1::m {
//            use 0x1::vector;
//
//            struct S { field: u8 }
//
//            fun main() {
//                let v = vector[S { field: 0 }];
//                (&mut v[0]).field;
//            }
//        }
//    """)

    private fun doTest(@Language("Move") text: String) =
        checkByText(text, checkWarn = false, checkWeakWarn = true)

    private fun doFixTest(
        @Language("Move") before: String,
        @Language("Move") after: String,
    ) =
        checkFixByText("Replace with index expr", before, after,
                       checkWarn = false, checkWeakWarn = true)
}