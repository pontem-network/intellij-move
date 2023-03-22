package org.move.lang.core.psi.ext

import org.move.lang.core.psi.MvAnnotatedExpr
import org.move.lang.core.psi.MvCallExpr
import org.move.lang.core.psi.MvCodeBlockExpr
import org.move.lang.core.psi.MvDotExpr
import org.move.lang.core.psi.MvExpr
import org.move.lang.core.psi.MvIndexExpr
import org.move.lang.core.psi.MvLambdaExpr
import org.move.lang.core.psi.MvLitExpr
import org.move.lang.core.psi.MvParensExpr
import org.move.lang.core.psi.MvRefExpr
import org.move.lang.core.psi.MvTupleLitExpr
import org.move.lang.core.psi.MvVectorLitExpr

val MvExpr.isAtomExpr: Boolean get() =
    this is MvAnnotatedExpr
            || this is MvTupleLitExpr
            || this is MvParensExpr
            || this is MvVectorLitExpr
            || this is MvDotExpr
            || this is MvIndexExpr
            || this is MvCallExpr
            || this is MvRefExpr
            || this is MvLambdaExpr
            || this is MvLitExpr
            || this is MvCodeBlockExpr
