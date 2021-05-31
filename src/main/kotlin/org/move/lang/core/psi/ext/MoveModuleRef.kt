package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import org.move.lang.core.psi.MoveImportedModuleRef
import org.move.lang.core.psi.MoveModuleRef
import org.move.lang.core.psi.impl.MoveReferenceElementImpl
import org.move.lang.core.resolve.ref.MoveModuleReferenceImpl
import org.move.lang.core.resolve.ref.MoveReference

val MoveModuleRef.isSelf: Boolean
    get() =
        this is MoveImportedModuleRef
                && this.referenceName == "Self"
                && this.containingModule != null

abstract class MoveModuleRefMixin(node: ASTNode) : MoveReferenceElementImpl(node),
                                                   MoveModuleRef {
    override fun getReference(): MoveReference {
        return MoveModuleReferenceImpl(this)
    }

    override val isUnresolved: Boolean
        get() = super<MoveReferenceElementImpl>.isUnresolved
}
