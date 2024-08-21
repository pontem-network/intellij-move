package org.move.lang.core.psi.ext

import org.move.lang.core.psi.MvMatchArgument
import org.move.lang.core.psi.MvMatchArm
import org.move.lang.core.psi.MvMatchBody
import org.move.lang.core.psi.MvMatchExpr

val MvMatchExpr.matchArgument: MvMatchArgument get() = childOfType<MvMatchArgument>()!!
val MvMatchExpr.matchBody: MvMatchBody get() = childOfType<MvMatchBody>()!!
val MvMatchExpr.arms: List<MvMatchArm> get() = matchBody.matchArmList
