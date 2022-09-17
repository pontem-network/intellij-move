package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import org.move.lang.core.psi.*

val MvItemSpec.item: MvNamedElement? get() = this.itemSpecRef?.reference?.resolve()

val MvItemSpec.funcItem get() = this.item as? MvFunction
val MvItemSpec.structItem get() = this.item as? MvStruct

val MvItemSpec.itemSpecBlock: MvItemSpecBlock? get() = this.childOfType()

abstract class MvItemSpecMixin(node: ASTNode) : MvElementImpl(node),
                                                MvItemSpec {
    override fun parameterBindings(): List<MvBindingPat> {
        return this.funcItem?.parameterBindings().orEmpty()
    }
}
