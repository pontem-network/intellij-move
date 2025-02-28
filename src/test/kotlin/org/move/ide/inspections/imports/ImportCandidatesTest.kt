package org.move.ide.inspections.imports

import org.move.ide.utils.imports.ImportCandidateCollector
import org.move.lang.core.psi.MvPath
import org.move.lang.core.resolve.ref.MvReferenceElement
import org.move.utils.tests.FileTreeBuilder
import org.move.utils.tests.MvProjectTestBase
import org.move.utils.tests.base.findElementWithDataAndOffsetInEditor

class ImportCandidatesTest : MvProjectTestBase() {
    fun `test cannot import test function`() = checkCandidates {
        namedMoveToml("Package")
        sources {
            move(
                "m1.move", """
module 0x1::m1 {
    #[test]
    public fun test_a() {}
}  
            """
            )
            main(
                """
module 0x1::main {
    #[test_only]
    fun main() {
        test_a();
        //^ []
    }
}
            """
            )
        }
    }

    fun `test cannot import private function`() = checkCandidates {
        namedMoveToml("Package")
        sources {
            move(
                "m1.move", """
module 0x1::m1 {
    fun call() {}
}  
            """
            )
            main(
                """
module 0x1::main {
    fun main() {
        call();
        //^ []
    }
}
            """
            )
        }
    }

    fun `test import public function`() = checkCandidates {
        namedMoveToml("Package")
        sources {
            move(
                "m1.move", """
module 0x1::m1 {
    public fun call() {}
}  
            """
            )
            main(
                """
module 0x1::main {
    fun main() {
        call();
        //^ 0x1::m1::call
    }
}
            """
            )
        }
    }

//    fun `test import entry function`() = checkImportCandidates {
//        namedMoveToml("Package")
//        sources {
//            move(
//                "m1.move", """
//module 0x1::m1 {
//    entry fun call() {}
//}
//            """
//            )
//            main(
//                """
//module 0x1::main {
//    entry fun main() {
//        call();
//        //^ 0x1::m1::call
//    }
//}
//            """
//            )
//        }
//    }

    fun `test import friend function`() = checkCandidates {
        namedMoveToml("Package")
        sources {
            move(
                "m1.move", """
module 0x1::m1 {
    friend 0x1::main;
    public(friend) fun call() {}
}  
            """
            )
            main(
                """
module 0x1::main {
    fun main() {
        call();
        //^ 0x1::m1::call
    }
}
            """
            )
        }
    }

    fun `test cannot import friend function if not a friend`() = checkCandidates {
        namedMoveToml("Package")
        sources {
            move(
                "m1.move", """
module 0x1::m1 {
    public(friend) fun call() {}
}  
            """
            )
            main(
                """
module 0x1::main {
    fun main() {
        call();
        //^ []
    }
}
            """
            )
        }
    }

    // TODO: test
//    fun `test not duplicate public method from dependency package included twice with different versions`() =
//        checkCandidates {
//            dir("aptos_framework") {
//                namedMoveToml("AptosFramework")
//                sources {
//                    main(
//                        """
//                        module 0x1::m {
//                            public fun aptos_call() {}
//                        }
//                    """
//                    )
//                }
//            }
//            dir("aptos_framework_new") {
//                namedMoveToml("AptosFramework")
//                sources {
//                    main(
//                        """
//                        module 0x1::m {
//                            public fun aptos_call() {}
//                        }
//                    """
//                    )
//                }
//            }
//            dir("bin_steps") {
//                moveToml(
//                    """
//                    [package]
//                    name = "BinSteps"
//
//                    [dependencies.AptosFramework]
//                    local = "../aptos_framework"
//                """
//                )
//                sources { }
//            }
//            moveToml(
//                """
//                [package]
//                name = "MyPackage"
//
//                [dependencies.AptosFramework]
//                local = "./aptos_framework_new"
//
//                [dependencies.BinSteps]
//                local = "./bin_steps"
//            """
//            )
//            sources {
//                main(
//                    """
//                    module 0x1::mm {
//                        public fun main() {
//                            aptos_call();
//                            //^ [0x1::m::aptos_call]
//                        }
//                    }
//                """
//                )
//            }
//        }

    fun `test no candidates for non module items if module fq path`() = checkCandidates {
        namedMoveToml("MyPackage")
        sources {
            move("option.move", """
                module 0x1::option {
                    struct Option<Element> has copy, drop, store {
                       vec: vector<Element>
                    }
                    public fun none<Element>(): Option<Element> {
                        Option { vec: vector[] }
                    }
                }
            """)
            move("delegation.move", """
                module 0x1::delegation {
                    public fun none_matched() {}
                }                
            """)
            move("main.move", """
                module 0x1::main {
                    use 0x1::option;
                    fun main() {
                        option::none()
                               //^ [0x1::option::none]
                    }
                }                                
            """)
        }
    }

    fun checkCandidates(tree: FileTreeBuilder.() -> Unit) {
        testProject(tree)

        val refClass = MvReferenceElement::class.java
        val (refElement, data, _) =
            myFixture.findElementWithDataAndOffsetInEditor(refClass, "^")
        val path = refElement as? MvPath ?: error("no path at caret")
        val targetName = path.referenceName ?: error("No name for reference element")

        val importContext = ImportContext.from(path, true) ?: error("no import context")
        val candidates =
            ImportCandidateCollector
                .getImportCandidates(importContext, targetName)
                .map { it.qualName.declarationText() }
        if (data == "[]") {
            check(candidates.isEmpty()) { "Non-empty candidates: $candidates" }
            return
        }

        val expectedCandidates = data.trim('[', ']').split(',')
            .takeIf { it.isNotEmpty() } ?: error("Invalid candidate set")
        check(candidates == expectedCandidates) {
            "Expected candidates: $expectedCandidates, actual: $candidates"
        }
    }
}
