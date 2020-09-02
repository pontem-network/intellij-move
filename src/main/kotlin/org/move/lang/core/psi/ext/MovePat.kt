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
                    field.pat?.accept(this) ?: elements.add(field)
                }
            }
        })
        return elements
    }