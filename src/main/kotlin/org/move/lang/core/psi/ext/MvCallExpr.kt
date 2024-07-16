package org.move.lang.core.psi.ext

import org.move.lang.core.psi.*

interface MvCallable: MvElement {
    val valueArgumentList: MvValueArgumentList?
}


val MvCallable.valueArguments: List<MvValueArgument>
    get() =
        this.valueArgumentList?.valueArgumentList.orEmpty()

val MvCallable.argumentExprs: List<MvExpr?> get() = this.valueArguments.map { it.expr }

