package org.move.utils

import org.move.stdext.joinToWithBuffer

object SignatureUtils {
    fun joinParameters(params: List<Pair<String, String?>>): String =
        buildString {
            append("(")
            params.joinToWithBuffer(this, separator = ", ") { sb ->
                val (name, type) = this
                sb.append(name)
                if (type != null) {
                    sb.append(": ")
                    sb.append(type)
                }
            }
            append(")")
        }
}