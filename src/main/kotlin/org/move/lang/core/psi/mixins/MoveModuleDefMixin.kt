package org.move.lang.core.psi.mixins

import com.intellij.lang.ASTNode
import com.intellij.psi.util.PsiTreeUtil
import org.move.lang.core.psi.*
import org.move.lang.core.psi.impl.MoveNameIdentifierOwnerImpl

abstract class MoveModuleDefMixin(node: ASTNode) : MoveNameIdentifierOwnerImpl(node),
                                                   MoveModuleDef {
    override fun definitions(): List<MoveNamedElement> {
        val functions = PsiTreeUtil.getChildrenOfTypeAsList(this.moduleBlock, MoveFunctionDef::class.java)
        val nativeFunctions =
            PsiTreeUtil.getChildrenOfTypeAsList(this.moduleBlock, MoveNativeFunctionDef::class.java)
        val structs = PsiTreeUtil.getChildrenOfTypeAsList(this.moduleBlock, MoveStructDef::class.java)
        val schemas = PsiTreeUtil.getChildrenOfTypeAsList(this.moduleBlock, MoveSchemaDef::class.java)
        val consts = PsiTreeUtil.getChildrenOfTypeAsList(this.moduleBlock, MoveConstDef::class.java)
        return listOf(
            functions, nativeFunctions, structs, schemas, consts
        ).flatten()
    }
}