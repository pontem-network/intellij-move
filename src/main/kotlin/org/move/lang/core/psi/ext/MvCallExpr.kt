package org.move.lang.core.psi.ext

import org.move.lang.core.psi.MvCallExpr
import org.move.lang.core.psi.MvElement
import org.move.lang.core.psi.MvExpr
import org.move.lang.core.psi.MvPath
import org.move.lang.core.psi.MvPathExpr
import org.move.lang.core.psi.MvValueArgument
import org.move.lang.core.psi.MvValueArgumentList

interface MvCallable: MvElement {
    val valueArgumentList: MvValueArgumentList?
}

val MvCallable.valueArguments: List<MvValueArgument>
    get() =
        this.valueArgumentList?.valueArgumentList.orEmpty()

val MvCallable.argumentExprs: List<MvExpr?> get() = this.valueArguments.map { it.expr }

val MvCallExpr.path: MvPath? get() = (this.expr as? MvPathExpr)?.path