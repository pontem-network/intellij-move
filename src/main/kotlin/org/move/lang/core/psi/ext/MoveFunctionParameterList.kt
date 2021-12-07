package org.move.lang.core.psi.ext

import org.move.lang.core.psi.MvFunctionParameterList
import org.move.utils.SignatureUtils

val MvFunctionParameterList.parametersText: String
    get() {
        return SignatureUtils.joinParameters(this.functionParameterList.map {
            Pair(it.bindingPat.name!!, it.typeAnnotation?.type?.text)
        })
    }
