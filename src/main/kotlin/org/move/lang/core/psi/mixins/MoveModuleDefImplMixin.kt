package org.move.lang.core.psi.mixins

import com.intellij.lang.ASTNode
import com.intellij.psi.util.PsiTreeUtil
import org.move.lang.core.psi.*
import org.move.lang.core.psi.impl.MoveNameIdentifierOwnerImpl

abstract class MoveModuleDefImplMixin(node: ASTNode) : MoveNameIdentifierOwnerImpl(node),
                                                       MoveModuleDef {
    override fun definitions(): List<MoveNamedElement> {
        val functionDefs = PsiTreeUtil.getChildrenOfTypeAsList(this.moduleBlock, MoveFunctionDef::class.java)
        val nativeFunctionDefs =
            PsiTreeUtil.getChildrenOfTypeAsList(this.moduleBlock, MoveNativeFunctionDef::class.java)
        val structDefs = PsiTreeUtil.getChildrenOfTypeAsList(this.moduleBlock, MoveStructDef::class.java)
        return listOf(
            functionDefs,
            nativeFunctionDefs,
            structDefs
        ).flatten()
    }
}