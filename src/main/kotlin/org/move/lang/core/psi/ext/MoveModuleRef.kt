package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import org.move.lang.core.psi.MoveElementImpl
import org.move.lang.core.psi.MoveFQModuleRef
import org.move.lang.core.psi.MoveModuleRef
import org.move.lang.core.resolve.ref.MoveModuleReferenceImpl
import org.move.lang.core.resolve.ref.MoveReference

val MoveModuleRef.isSelf: Boolean
    get() =
        this !is MoveFQModuleRef
                && this.referenceName == "Self"
                && this.containingModule != null

abstract class MoveModuleRefMixin(node: ASTNode) : MoveElementImpl(node), MoveModuleRef {
    override fun getReference(): MoveReference? {
        return MoveModuleReferenceImpl(this)
    }
}

//abstract class MoveImportedModuleRefMixin(node: ASTNode) : MoveReferenceElementImpl(node),
//                                                           MoveImportedModuleRef {
//    override val identifier: PsiElement
//        get() {
//            throw NotImplementedError()
////            if (this is MoveImportedModuleRef) return this.identifier
////            if (this is MoveFQModuleRef) return this.identifier
//////            if (self is MoveImportedModuleRef
//////                || self is MoveFQModuleRef) return self.identifier
////            return null
//        }
//
//    override fun getReference(): MoveReference {
//        return MoveModuleReferenceImpl(this)
//    }
//
//    override val isUnresolved: Boolean
//        get() = super<MoveReferenceElementImpl>.isUnresolved
//}
