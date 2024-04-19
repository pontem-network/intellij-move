package org.move.lang.core.psi.ext

import org.move.lang.core.psi.MvDotExpr
import org.move.lang.core.psi.MvExpr
import org.move.lang.core.resolve.ref.MvMandatoryReferenceElement

interface MvMethodOrField : MvMandatoryReferenceElement

val MvMethodOrField.parentDotExpr: MvDotExpr get() = parent as MvDotExpr
val MvMethodOrField.receiverExpr: MvExpr get() = parentDotExpr.expr
