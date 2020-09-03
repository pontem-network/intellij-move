package org.move.lang.core.psi.ext

import org.move.lang.core.psi.MoveFunctionParameterList
import org.move.stdext.joinToWithBuffer

val MoveFunctionParameterList.compactText: String
    get() {
        val params = this.functionParameterList
        return buildString {
            append("(")
            params.joinToWithBuffer(this, separator = ", ") { sb ->
                sb.append(this.name ?: "_")
                val paramType = this.type
                if (paramType != null) {
                    sb.append(": ")
                    sb.append(paramType.text)
                }
            }
            append(")")
        }
//        val params = this.functionParameterList.orEmpty()
////        if (params.isEmpty()) {
////            return "()"
////        }
//        params.joinToString(separator = ", ") { sb ->
//        }

    }