package org.move.lang.core.psi.ext

import com.intellij.psi.PsiElement
import org.move.lang.MvElementTypes.MATCH_KW
import org.move.lang.core.psi.MvMatchArgument
import org.move.lang.core.psi.MvMatchArm
import org.move.lang.core.psi.MvMatchBody
import org.move.lang.core.psi.MvMatchExpr

val MvMatchExpr.matchKw: PsiElement? get() = findFirstChildByType(MATCH_KW)

val MvMatchExpr.matchArgument: MvMatchArgument get() = childOfType<MvMatchArgument>()!!
val MvMatchExpr.matchBody: MvMatchBody get() = childOfType<MvMatchBody>()!!
val MvMatchExpr.arms: List<MvMatchArm> get() = matchBody.matchArmList
