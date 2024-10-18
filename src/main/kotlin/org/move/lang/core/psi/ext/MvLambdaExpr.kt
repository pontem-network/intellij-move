package org.move.lang.core.psi.ext

import org.move.lang.core.psi.MvLambdaExpr
import org.move.lang.core.psi.MvLambdaParameter
import org.move.lang.core.psi.MvPatBinding

val MvLambdaExpr.parameters: List<MvLambdaParameter> get() = this.lambdaParameterList.lambdaParameterList
val MvLambdaExpr.parametersAsBindings: List<MvPatBinding> get() = parameters.map { it.patBinding }