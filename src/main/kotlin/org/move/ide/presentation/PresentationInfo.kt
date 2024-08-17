package org.move.ide.presentation

import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.owner

class PresentationInfo(
    val element: MvNamedElement,
    val type: String,
    val name: String
)

val MvNamedElement.presentationInfo: PresentationInfo?
    get() {
        val elementName = name ?: return null

        val type = when (this) {
            is MvTypeParameter -> "type parameter"
            is MvPatBinding -> {
                val owner = this.owner
                when (owner) {
                    is MvFunctionParameter -> "value parameter"
                    is MvLetStmt -> "variable"
                    else -> return null
                }
            }
            else -> return null
        }
        return PresentationInfo(this, type, elementName)
    }
