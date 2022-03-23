package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import org.move.lang.core.psi.*
import org.move.lang.core.resolve.ref.MvReference
import org.move.lang.core.resolve.ref.MvNameSpecDefReferenceImpl

val MvItemSpec.item: MvNamedElement? get() = this.reference.resolve()

val MvItemSpec.funcItem get() = this.item as? MvFunction
val MvItemSpec.structItem get() = this.item as? MvStruct

val MvItemSpec.specBlock: MvSpecBlock? get() = this.childOfType()
val MvModuleSpec.specBlock: MvSpecBlock? get() = this.childOfType()

abstract class MvItemSpecMixin(node: ASTNode) : MvElementImpl(node), MvItemSpec {
    override fun getReference(): MvReference {
        return MvNameSpecDefReferenceImpl(this)
    }
}
