package org.move.lang.core.psi

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiUtilBase
import org.move.lang.MoveFile
import org.move.lang.core.psi.ext.ancestorOrSelf
import org.move.lang.core.psi.ext.ancestorStrict
import org.move.lang.core.psi.ext.findFirstParent
import org.move.lang.core.psi.ext.module

interface MvElement : PsiElement

abstract class MvElementImpl(node: ASTNode) : ASTWrapperPsiElement(node),
                                              MvElement

val MvElement.containingMoveFile: MoveFile? get() = this.containingFile as? MoveFile

val MvElement.containingScript: MvScript? get() = ancestorStrict()

val MvElement.containingFunction: MvFunction? get() = ancestorStrict()

val MvElement.containingFunctionLike: MvFunctionLike? get() = ancestorStrict()

val MvElement.namespaceModule: MvModule? get() {
    val parent = this.findFirstParent(false) { it is MvModule || it is MvModuleSpec }
    return when (parent) {
        is MvModule -> parent
        is MvModuleSpec -> parent.module
        else -> null
    }
}

val MvElement.containingModule: MvModule? get() = ancestorStrict()

val MvElement.containingImportsOwner get() = ancestorOrSelf<MvImportsOwner>()

val MvElement.containingModuleOrScript: MvElement?
    get() {
        return this.findFirstParent(false) { it is MvScript || it is MvModule }
                as? MvElement
    }
