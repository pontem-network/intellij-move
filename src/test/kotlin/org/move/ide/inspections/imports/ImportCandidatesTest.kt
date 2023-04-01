package org.move.ide.inspections.imports

import org.move.lang.core.resolve.MvReferenceElement
import org.move.utils.tests.FileTreeBuilder
import org.move.utils.tests.MvProjectTestBase
import org.move.utils.tests.base.findElementWithDataAndOffsetInEditor

class ImportCandidatesTest : MvProjectTestBase() {
    fun `test cannot import test function`() = checkImportCandidates {
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

    fun `test cannot import private function`() = checkImportCandidates {
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

    fun `test import public function`() = checkImportCandidates {
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

    fun `test import friend function`() = checkImportCandidates {
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

    fun `test cannot import friend function if not a friend`() = checkImportCandidates {
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

    fun checkImportCandidates(
        tree: FileTreeBuilder.() -> Unit
    ) {
        testProject(tree)

        val refClass = MvReferenceElement::class.java
        val (refElement, data, _) =
            myFixture.findElementWithDataAndOffsetInEditor(refClass, "^")
        val targetName = refElement.referenceName ?: error("No name for reference element")

        val candidates =
            AutoImportFix.getImportCandidates(ImportContext.Companion.from(refElement), targetName)
                .map { it.qualName.editorText() }
        if (data == "[]") {
            check(candidates.isEmpty()) { "Non-empty candidates: $candidates" }
            return
        }

        val expectedCandidates = data.split(',')
            .takeIf { it.isNotEmpty() } ?: error("Invalid candidate set")
        check(candidates == expectedCandidates) {
            "Expected candidates: $expectedCandidates, actual: $candidates"
        }
    }
}
