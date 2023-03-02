package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import com.intellij.openapi.util.SimpleModificationTracker
import com.intellij.psi.PsiElement
import org.move.lang.core.psi.*

val MvItemSpec.item: MvNamedElement? get() = this.itemSpecRef?.reference?.resolve()

val MvItemSpec.funcItem get() = this.item as? MvFunction

val MvModuleItemSpec.itemSpecBlock: MvItemSpecBlock? get() = this.childOfType()
val MvItemSpec.itemSpecBlock: MvItemSpecBlock? get() = this.childOfType()

abstract class MvItemSpecMixin(node: ASTNode) : MvElementImpl(node),
                                                MvItemSpec {
    override fun parameterBindings(): List<MvBindingPat> {
        return this.funcItem?.parameterBindings().orEmpty()
    }

    override val modificationTracker: SimpleModificationTracker = SimpleModificationTracker()

    override fun incModificationCount(element: PsiElement): Boolean {
//        val shouldInc = element.ancestors.any {
//            it == this.itemSpecBlock
//                    || it == this.itemSpecSignature
//        }
        val shouldInc = this.itemSpecBlock?.isAncestorOf(element) == true
        if (shouldInc)
            modificationTracker.incModificationCount()
        return shouldInc
    }
}
