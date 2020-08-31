package org.move.lang.core.resolve.ref

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase
import org.move.ide.refactoring.isValidMoveVariableIdentifier
import org.move.lang.MoveElementTypes.IDENTIFIER
import org.move.lang.core.psi.MoveNamedElement
import org.move.lang.core.psi.MovePsiFactory
import org.move.lang.core.psi.MoveReferenceElement
import org.move.lang.core.psi.ext.elementType
import org.move.lang.core.resolve.ResolveEngine

abstract class MoveReferenceBase<T : MoveReferenceElement>(element: T) : PsiReferenceBase<T>(element),
                                                                         MoveReference {
    abstract fun resolveVerbose(): ResolveEngine.ResolveResult

    final override fun resolve(): MoveNamedElement? =
        resolveVerbose().let {
            when (it) {
                is ResolveEngine.ResolveResult.Resolved -> it.element
                else -> null
            }
        }

    open val T.referenceAnchor: PsiElement get() = referenceNameElement

    final override fun calculateDefaultRangeInElement(): TextRange {
        val anchor = element.referenceAnchor
        return TextRange.from(anchor.startOffsetInParent, anchor.textLength)
    }

    override fun handleElementRename(newElementName: String): PsiElement {
        val refNameElement = element.referenceNameElement
        doRename(refNameElement, newElementName)
        return element
    }

    companion object {
        @JvmStatic
        protected fun doRename(identifier: PsiElement, newName: String) {
            val factory = MovePsiFactory(identifier.project)
            val newIdentifier = when (identifier.elementType) {
                IDENTIFIER -> {
                    if (!isValidMoveVariableIdentifier(newName)) return
                    factory.createIdentifier(newName)
                }
                else -> error("Unsupported identifier type for `$newName` (${identifier.elementType})")
            }
            identifier.replace(newIdentifier)
        }
    }
}