package org.move.lang.core.psi.ext

import com.intellij.psi.PsiElement
import org.move.lang.MvElementTypes.IDENTIFIER
import org.move.lang.core.psi.MvAssertMacroExpr

val MvAssertMacroExpr.identifier: PsiElement get() = this.findFirstChildByType(IDENTIFIER)!!