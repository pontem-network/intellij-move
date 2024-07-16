package org.move.lang.resolve

import org.move.utils.tests.resolve.ResolveProjectTestCase

class ResolveStubOnlyTest : ResolveProjectTestCase() {
    fun `test stub resolve module import`() = stubOnlyResolve {
        namedMoveToml("MyPackage")
        sources {
            move(
                "module.move", """
            module 0x1::Module {}
                      //X
            """
            )
            move(
                "main.move", """
            script {
                use 0x1::Module;
                       //^
            }
            """
            )
        }
    }

    fun `test stub resolve module ref`() = stubOnlyResolve {
        namedMoveToml("MyPackage")
        sources {
            move(
                "module.move", """
            module 0x1::Module {
                        //X
                public fun call() {}
            }
            """
            )
            move(
                "main.move", """
            script {
                use 0x1::Module;
                fun main() {
                    Module::call();
                    //^
                }                       
            }
            """
            )
        }
    }

//    fun `test stub resolve module function`() = stubOnlyResolve {
//        namedMoveToml("MyPackage")
//        sources {
//            move(
//                "module.move", """
//            module 0x1::Module {
//                public fun call() {}
//                          //X
//            }
//            """
//            )
//            move(
//                "main.move", """
//            script {
//                use 0x1::Module;
//                fun main() {
//                    Module::call();
//                          //^
//                }
//            }
//            """
//            )
//        }
//    }

//    fun `test stub resolve fq module function`() = stubOnlyResolve {
//        namedMoveToml("MyPackage")
//        sources {
//            move(
//                "module.move", """
//            module 0x1::Module {
//                public fun call() {}
//                          //X
//            }
//            """
//            )
//            move(
//                "main.move", """
//            script {
//                fun main() {
//                    0x1::Module::call();
//                               //^
//                }
//            }
//            """
//            )
//        }
//    }

//    fun `test stub resolve module struct`() = stubOnlyResolve {
//        namedMoveToml("MyPackage")
//        sources {
//            move(
//                "module.move", """
//            module 0x1::Module {
//                struct S {}
//                     //X
//            }
//            """
//            )
//            move(
//                "main.move", """
//            script {
//                use 0x1::Module;
//                fun main(s: Module::S) {
//                                  //^
//                }
//            }
//            """
//            )
//        }
//    }
}
