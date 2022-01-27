package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import org.move.lang.core.psi.*
import org.move.lang.core.resolve.ref.MvReference
import org.move.lang.core.resolve.ref.MvNameSpecDefReferenceImpl

val MvNameSpecDef.item: MvNamedElement? get() = this.reference.resolve()

val MvNameSpecDef.funcItem get() = this.item as? MvFunction
val MvNameSpecDef.structItem get() = this.item as? MvStruct

abstract class MvNameSpecDefMixin(node: ASTNode) : MvElementImpl(node), MvNameSpecDef {
    override fun getReference(): MvReference {
        return MvNameSpecDefReferenceImpl(this)
    }
}
