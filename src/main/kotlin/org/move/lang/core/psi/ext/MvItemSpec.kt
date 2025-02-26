package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.move.lang.core.psi.*
import org.move.stdext.chain

val MvItemSpec.item: MvNamedElement? get() = this.itemSpecRef?.reference?.resolve()

val MvItemSpec.funcItem get() = this.item as? MvFunction

val MvModuleItemSpec.itemSpecBlock: MvSpecCodeBlock? get() = this.childOfType()

val MvItemSpec.module: MvModule?
    get() {
        val parent = this.parent
        return when (parent) {
            is MvModule -> parent
            is MvModuleSpecBlock -> parent.moduleSpec.moduleItem
            else -> error("unreachable")
        }
    }

/**
 * `spec module {}` item
 */
fun MvModuleItemSpec.specInlineFunctions(): List<MvSpecInlineFunction> {
    return this.itemSpecBlock?.stmtList
        ?.filterIsInstance<MvSpecInlineFunctionStmt>()
        ?.map { it.specInlineFunction }
        .orEmpty()
}

val MvItemSpec.itemSpecBlock: MvSpecCodeBlock? get() = this.childOfType()
val MvSpecBlockExpr.specBlock: MvSpecCodeBlock? get() = this.childOfType()

abstract class MvItemSpecMixin(node: ASTNode): MvElementImpl(node),
                                               MvItemSpec {

    override val modificationTracker = MvModificationTracker(this)

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
