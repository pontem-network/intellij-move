package org.move.ide.annotator.syntaxErrors

// TODO: test
// TODO: global is not allowed in schemas
// TODO: local / schema variables are not allowed in spec blocks
// TODO: update only available in inline specs
//class AllowedSpecStatementsTest: AnnotatorTestCase(MvSyntaxErrorAnnotator::class) {
//    fun `test assert assume allowed in inline spec blocks`() = checkWarnings("""
//        module 0x1::m {
//            fun main() {
//                spec {
//                    assert 1 == 1;
//                    assume 1 == 1;
//                }
//            }
//        }
//    """)
//
//    fun `test assert assume forbidden in spec function blocks`() = checkWarnings("""
//        module 0x1::m {
//            fun main() {}
//            spec main {
//                <error descr="'assert' is not allowed in item specs">assert</error> 1 == 1;
//                <error descr="'assume' is not allowed in item specs">assume</error> 1 == 1;
//            }
//        }
//    """)
//}
