package org.move.lang.core.psi.ext

import org.move.lang.core.psi.MvFunctionParameter
import org.move.utils.SignatureUtils

fun List<MvFunctionParameter>.joinToSignature(): String {
    val parameterPairs = this.map { Pair(it.patBinding.name, it.type?.text) }
    return SignatureUtils.joinParameters(parameterPairs)
}
