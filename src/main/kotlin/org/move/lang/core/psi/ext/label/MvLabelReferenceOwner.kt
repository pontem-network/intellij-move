package org.move.lang.core.psi.ext.label

import com.intellij.psi.PsiElement
import org.move.lang.core.psi.MvElement
import org.move.lang.core.psi.MvLabel

interface MvLabelReferenceOwner: MvElement {
    /**
     * Returns `break` in case of [MvBreakExpr] and `continue` in case of [MvContinueExpr]
     */
    val operator: PsiElement
    val label: MvLabel?

}