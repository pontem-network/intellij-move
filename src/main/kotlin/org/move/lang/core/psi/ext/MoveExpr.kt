package org.move.lang.core.psi.ext

import org.move.lang.core.psi.MoveExpr
import org.move.lang.core.psi.MoveRefExpr
import org.move.lang.core.psi.MoveTypeAnnotated
import org.move.lang.core.psi.type
import org.move.lang.core.types.BaseType

val MoveExpr.type: BaseType?
    get() {
        return when (this) {
            is MoveRefExpr -> {
                val referred = this.reference?.resolve() ?: return null
                if (referred !is MoveTypeAnnotated) return null
                referred.type?.resolvedType
            }
            else -> null
        }
    }
