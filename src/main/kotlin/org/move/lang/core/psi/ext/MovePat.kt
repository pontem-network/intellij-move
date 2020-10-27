package org.move.lang.core.psi.ext

import org.move.lang.core.psi.*

val MovePat.boundElements: List<MoveNamedElement>
    get() {
        val elements = mutableListOf<MoveNamedElement>()
        accept(object : MoveVisitor() {
            override fun visitBindingPat(o: MoveBindingPat) {
                elements.add(o)
            }

            override fun visitStructPat(o: MoveStructPat) {
                o.structPatFieldsBlock.structPatFieldList.forEach { field ->
                    field.structPatFieldBinding?.pat?.accept(this) ?: elements.add(field)
                }
            }

            override fun visitTuplePat(o: MoveTuplePat) {
                o.patList.forEach { it.accept(this) }
            }
        })
        return elements
    }