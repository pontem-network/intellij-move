package org.move.lang.core.psi

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.move.lang.MoveFile
import org.move.lang.core.psi.ext.ancestorOrSelf
import org.move.lang.core.psi.ext.ancestorStrict
import org.move.lang.core.psi.ext.findFirstParent

interface MvElement : PsiElement

abstract class MvElementImpl(node: ASTNode) : ASTWrapperPsiElement(node),
                                              MvElement

val MvElement.containingMoveFile: MoveFile? get() = this.containingFile as? MoveFile

val MvElement.containingScript: MvScript? get() = ancestorStrict()

val MvElement.containingFunction: MvFunction? get() = ancestorStrict()

val MvElement.containingFunctionLike: MvFunctionLike? get() = ancestorStrict()

val MvElement.containingModule: MvModule? get() = ancestorStrict()

val MvElement.containingImportsOwner get() = ancestorOrSelf<MvImportsOwner>()

val MvElement.containingModuleOrScript: MvElement?
    get() {
        return this.findFirstParent(false) { it is MvScript || it is MvModule }
                as? MvElement
    }
