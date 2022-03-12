package org.move.lang.core.psi

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.move.lang.MvFile
import org.move.lang.core.psi.ext.ancestorOrSelf
import org.move.lang.core.psi.ext.ancestorStrict

interface MvElement : PsiElement

abstract class MvElementImpl(node: ASTNode) : ASTWrapperPsiElement(node),
                                              MvElement

val MvElement.containingMvFile: MvFile? get() = this.containingFile as? MvFile

val MvElement.containingScript: MvScriptDef? get() = ancestorStrict()

val MvElement.containingFunction: MvFunction? get() = ancestorStrict()

val MvElement.containingFunctionLike: MvFunctionLike? get() = ancestorStrict()

val MvElement.containingModule: MvModuleDef? get() = ancestorStrict()

val MvElement.containingImportsOwner get() = ancestorOrSelf<MvImportsOwner>()
