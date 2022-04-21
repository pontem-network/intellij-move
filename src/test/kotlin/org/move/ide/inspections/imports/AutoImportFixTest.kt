package org.move.ide.inspections.imports

import org.intellij.lang.annotations.Language
import org.move.ide.inspections.MvUnresolvedReferenceInspection
import org.move.utils.tests.annotation.InspectionTestBase

class AutoImportFixTest : InspectionTestBase(MvUnresolvedReferenceInspection::class) {
    fun `test method`() = checkAutoImportFixByText(
        """
module 0x1::M {
    public fun call() {}
}
script {
    fun main() {
        <error descr="Unresolved reference: `call`">/*caret*/call</error>();
    }
}
    """,
        """
module 0x1::M {
    public fun call() {}
}
script {
    use 0x1::M::call;

    fun main() {
        call();
    }
}
    """
    )

    fun `test module`() = checkAutoImportFixByText(
        """
module 0x1::Signer {
    public fun address_of() {}
}
script {
    fun main() {
        <error descr="Unresolved module reference: `Signer`">/*caret*/Signer</error>::address_of();
    }
}
    """,
        """
module 0x1::Signer {
    public fun address_of() {}
}
script {
    use 0x1::Signer;

    fun main() {
        Signer::address_of();
    }
}
    """
    )

    fun `test unavailable if unresolved member`() = checkAutoImportFixIsUnavailable("""
module 0x1::M {}
module 0x1::Main {
    use 0x1::M;
    
    fun main() {
        M::<error descr="Unresolved reference: `value`">/*caret*/value</error>();
    }
}        
    """)

    private fun checkAutoImportFixByText(
        @Language("Move") before: String,
        @Language("Move") after: String,
    ) = doTest { checkFixByText(AutoImportFix.NAME, before, after) }

//    protected fun checkAutoImportFixByFileTree(
//        before: FileTreeBuilder.() -> Unit,
//        after: FileTreeBuilder.() -> Unit,
//    ) = doTest { checkFixByFileTree(AutoImportFix.NAME, before, after) }

    private fun checkAutoImportFixIsUnavailable(@Language("Move") text: String) =
        doTest { checkFixIsUnavailable(AutoImportFix.NAME, text) }

    protected fun checkAutoImportFixByTextWithMultipleChoice(
        @Language("Move") before: String,
        expectedElements: Set<String>,
        choice: String,
        @Language("Move") after: String
    ) = doTest {
        var chooseItemWasCalled = false

        withMockImportItemUi(object : ImportItemUi {
            override fun chooseItem(items: List<ImportCandidate>, callback: (ImportCandidate) -> Unit) {
                chooseItemWasCalled = true
                val actualItems = items.mapTo(HashSet()) { it.usePath }
                assertEquals(expectedElements, actualItems)
                val selectedValue = items.find { it.usePath == choice }
                    ?: error("Can't find `$choice` in `$actualItems`")
                callback(selectedValue)
            }
        }) { checkFixByText(AutoImportFix.NAME, before, after) }

        check(chooseItemWasCalled) { "`chooseItem` was not called" }
    }

    private inline fun doTest(action: () -> Unit) {
        val inspection = inspection as MvUnresolvedReferenceInspection
        val defaultValue = inspection.ignoreWithoutQuickFix
        try {
            inspection.ignoreWithoutQuickFix = false
            action()
        } finally {
            inspection.ignoreWithoutQuickFix = defaultValue
        }
    }
}
