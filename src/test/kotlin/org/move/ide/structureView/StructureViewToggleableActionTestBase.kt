/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.move.ide.structureView

import org.intellij.lang.annotations.Language

abstract class StructureViewToggleableActionTestBase : StructureViewTestBase() {
    protected abstract val actionId: String

    protected fun doTest(@Language("Move") code: String, disabled: String, enabled: String) =
        doTestStructureView(code) {
            setActionActive(actionId, false)
            assertTreeEqual(tree, disabled.trimMargin())
            setActionActive(actionId, true)
            assertTreeEqual(tree, enabled.trimMargin())
        }
}
