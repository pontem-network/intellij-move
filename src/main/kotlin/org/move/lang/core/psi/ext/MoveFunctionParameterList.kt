package org.move.lang.core.psi.ext

import org.move.lang.core.psi.MoveFunctionParameterList
import org.move.lang.core.psi.type
import org.move.utils.SignatureUtils

val MoveFunctionParameterList.compactText: String
    get() {
        return SignatureUtils.joinParameters(this.functionParameterList.map {
            Pair(it.name!!, it.type?.text)
        })
    }