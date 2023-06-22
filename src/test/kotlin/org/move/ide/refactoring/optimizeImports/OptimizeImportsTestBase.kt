package org.move.ide.refactoring.optimizeImports

import org.intellij.lang.annotations.Language
import org.move.ide.inspections.MvUnusedImportInspection
import org.move.utils.tests.WithEnabledInspections
import org.move.utils.tests.MvTestBase

@WithEnabledInspections(MvUnusedImportInspection::class)
abstract class OptimizeImportsTestBase: MvTestBase() {

    protected fun doTest(@Language("Move") before: String, @Language("Move") after: String) =
        checkEditorAction(before, after, "OptimizeImports")
}
