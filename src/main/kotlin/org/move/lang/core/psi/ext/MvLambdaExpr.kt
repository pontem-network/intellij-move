package org.move.lang.core.psi.ext

import org.move.lang.core.psi.MvLambdaExpr
import org.move.lang.core.psi.MvLambdaParameter
import org.move.lang.core.psi.MvPatBinding

val MvLambdaExpr.lambdaParameters: List<MvLambdaParameter> get() = this.lambdaParameterList.lambdaParameterList
val MvLambdaExpr.lambdaParametersAsBindings: List<MvPatBinding> get() = lambdaParameters.map { it.patBinding }