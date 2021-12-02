package org.move.ide.presentation

import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.owner

class PresentationInfo(
    val element: MoveNamedElement,
    val type: String,
    val name: String
)

val MoveNamedElement.presentationInfo: PresentationInfo?
    get() {
        val elementName = name ?: return null

        val type = when (this) {
            is MoveTypeParameter -> "type parameter"
            is MoveBindingPat -> {
                val owner = this.owner
                when (owner) {
                    is MoveFunctionParameter -> "value parameter"
                    is MoveLetStatement -> "variable"
                    else -> return null
                }
            }
            else -> return null
        }
        return PresentationInfo(this, type, elementName)
    }
