package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.move.lang.core.psi.*
import org.move.stdext.chain

val MvItemSpec.item: MvNamedElement? get() = this.itemSpecRef?.reference?.resolve()

val MvItemSpec.funcItem get() = this.item as? MvFunction

val MvModuleItemSpec.itemSpecBlock: MvSpecCodeBlock? get() = this.childOfType()

val MvModule.mslSpecifiableItems: List<MvNameIdentifierOwner>
    get() =
        this.allFunctions().chain(this.structs()).toList()

val MvItemSpec.module: MvModule?
    get() {
        val block = this.parent
        return when (block) {
            is MvModuleBlock -> block.module
            is MvModuleSpecBlock -> block.moduleSpec.moduleItem
            else -> error("unreachable")
        }
    }

fun MvModuleItemSpec.specInlineFunctions(): List<MvSpecInlineFunction> =
    this.itemSpecBlock?.stmtList
        ?.filterIsInstance<MvSpecInlineFunctionStmt>()
        ?.map { it.specInlineFunction }
        .orEmpty()

val MvItemSpec.itemSpecBlock: MvSpecCodeBlock? get() = this.childOfType()
val MvItemSpecBlockExpr.specBlock: MvSpecCodeBlock? get() = this.childOfType()

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
