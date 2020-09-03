package org.move.ide.inspections

//class LivenessInspectionTest : InspectionsTestCase(LivenessInspection::class) {
//    fun `test dead uninit variable`() = checkByText("""
//        module M {
//            fun main() {
//                let <warning descr="Variable `x` is never used">x</warning>;
//            }
//        }
//    """)
//
//    fun `test dead init variable`() = checkByText("""
//        module M {
//            fun main() {
//                let <warning descr="Variable `x` is never used">x</warning> = 1;
//            }
//        }
//    """)
//
//    fun `test live variable expr statement`() = checkByText("""
//        module M {
//            fun main() {
//                let y = 5;
//                y;
//            }
//        }
//    """)
//
//    fun `test live variable return expr`() = checkByText("""
//        module M {
//            fun main() {
//                let y = 5;
//                y + 5
//            }
//        }
//    """)
//}