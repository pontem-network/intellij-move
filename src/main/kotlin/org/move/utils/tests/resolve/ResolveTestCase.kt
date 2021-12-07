package org.move.utils.tests.resolve

import org.intellij.lang.annotations.Language
import org.move.lang.core.psi.MvNamedElement
import org.move.lang.core.psi.MvReferenceElement
import org.move.utils.tests.MvTestBase
import org.move.utils.tests.base.findElementInEditor
import org.move.utils.tests.base.findElementWithDataAndOffsetInEditor

abstract class ResolveTestCase : MvTestBase() {
    protected fun checkByCode(
        @Language("Move") code: String,
    ) {
        inlineFile(code, "main.move")

        val (refElement, data, offset) = myFixture.findElementWithDataAndOffsetInEditor<MvReferenceElement>(
            "^"
        )

        if (data == "unresolved") {
            val resolved = refElement.reference?.resolve()
            check(resolved == null) {
                "$refElement `${refElement.text}`should be unresolved, was resolved to\n$resolved `${resolved?.text}`"
            }
            return
        }

        val resolved = refElement.checkedResolve(offset)

        val target = myFixture.findElementInEditor(MvNamedElement::class.java, "X")
        check(resolved == target) {
            "$refElement `${refElement.text}` should resolve to $target (${target.text}), was $resolved (${resolved.text}) instead"
        }
    }
}
