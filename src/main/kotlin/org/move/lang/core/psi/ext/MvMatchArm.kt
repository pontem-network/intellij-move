package org.move.lang.core.psi.ext

import org.move.lang.core.psi.MvMatchArm
import org.move.lang.core.psi.MvMatchBody
import org.move.lang.core.psi.MvMatchExpr

val MvMatchArm.matchBody: MvMatchBody get() = this.parent as MvMatchBody
val MvMatchArm.matchExpr: MvMatchExpr get() = this.matchBody.parent as MvMatchExpr
