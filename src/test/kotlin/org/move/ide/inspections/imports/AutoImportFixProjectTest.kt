package org.move.ide.inspections.imports

import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.indexing.FileBasedIndex
import org.intellij.lang.annotations.Language
import org.move.ide.inspections.MvUnresolvedReferenceInspection
import org.move.utils.tests.FileTreeBuilder
import org.move.utils.tests.annotation.InspectionProjectTestBase

class AutoImportFixProjectTest : InspectionProjectTestBase(MvUnresolvedReferenceInspection::class) {
    fun `test import method from another file`() = checkAutoImportFixByText(
        {
            moveToml("""""")
            sources {
                move(
                    "Mod.move", """
            module 0x1::Mod {
                public fun call() {}
            }    
            """
                )
                move(
                    "Main.move", """
            module 0x1::Main {
                fun main() {
                    <error descr="Unresolved reference: `call`">/*caret*/call</error>();
                }
            }    
            """
                )
            }
        }, """
            module 0x1::Main {
                use 0x1::Mod::call;

                fun main() {
                    call();
                }
            }    
        """)

    private fun checkAutoImportFixByText(
        before: FileTreeBuilder.() -> Unit,
        @Language("Move") after: String,
    ) = doTest { checkFixByFileTree(AutoImportFix.NAME, before, after) }

    private inline fun doTest(action: () -> Unit) {
        val fileIndex = FileBasedIndex.getInstance()
        fileIndex.ensureUpToDate(MvNamedElementIndex.KEY, project, GlobalSearchScope.allScope(project))

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
