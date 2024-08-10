package org.move.utils.tests.resolve

import org.intellij.lang.annotations.Language
import org.move.lang.core.psi.MvElement
import org.move.lang.core.psi.MvNamedElement
import org.move.lang.core.psi.containingModule
import org.move.lang.core.resolve.ref.MvReferenceElement
import org.move.utils.tests.MvTestBase
import org.move.utils.tests.base.findElementInEditor
import org.move.utils.tests.base.findElementWithDataAndOffsetInEditor

abstract class ResolveTestCase : MvTestBase() {
    protected fun checkByCode(
        @Language("Move") code: String,
    ) {
        InlineFile(code, "main.move")

        val (refElement, data, offset) =
            myFixture.findElementWithDataAndOffsetInEditor<MvReferenceElement>("^")

        if (data == "unresolved") {
            val resolved = refElement.reference?.resolve()
            check(resolved == null) {
                "$refElement `${refElement.text}`should be unresolved, was resolved to\n$resolved `${resolved?.text}`"
            }
            return
        }

        val resolved = refElement.checkedResolve(offset)

        if (data.startsWith("spec_builtins::")) {
            check(resolved is MvNamedElement)
            val resolvedModule = resolved.containingModule
            check(resolvedModule?.name == "spec_builtins") {
                "Resolved element ${resolved.text} does not belong to 'spec_builtins'"
            }
            val targetName = data.substringAfter("spec_builtins::")
            check(resolved.name == targetName) {
                "$refElement `${refElement.text}` should resolve to $targetName, was $resolved (${resolved.text}) instead"
            }
            return
        }

        val target = myFixture.findElementInEditor(MvNamedElement::class.java, "X")
        check(resolved == target) {
            "$refElement `${refElement.text}` should resolve to $target (${target.text}), was $resolved (${resolved.text}) instead"
        }
    }
}
