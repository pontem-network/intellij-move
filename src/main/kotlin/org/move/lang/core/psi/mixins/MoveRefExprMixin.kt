package org.move.lang.core.psi.mixins

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.move.lang.MoveElementTypes
import org.move.lang.core.psi.MoveRefExpr
import org.move.lang.core.psi.ext.identifierNameElement
import org.move.lang.core.psi.impl.MoveElementImpl
import org.move.lang.core.resolve.ref.MoveReference
import org.move.lang.core.resolve.ref.MoveReferenceImpl

//interface MovePathExprReference : MoveReference
//
//class MovePathExprReferenceImpl(element: MovePathExpr) : PsiReferenceBase<MovePathExpr>(element),
//                                                         MovePathExprReference {
//    override fun resolve(): MoveNamedElement? = null
//}

abstract class MoveRefExprMixin(node: ASTNode) : MoveElementImpl(node),
                                                 MoveRefExpr {
    override val referenceNameElement: PsiElement
        get() = qualifiedPath.identifierNameElement
//    override val referenceNameElement: PsiElement
//        get() = checkNotNull(this.findLastChildByType(MoveElementTypes.IDENTIFIER)) {
//            "Path must contain identifier: $this `${this.text}` at `${this.containingFile.virtualFile.path}`"
//        }

    override fun getReference(): MoveReference =
        MoveReferenceImpl(this)
}