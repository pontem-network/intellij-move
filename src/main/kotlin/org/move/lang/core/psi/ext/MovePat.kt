package org.move.lang.core.psi.ext

import org.move.lang.core.psi.MoveBindingPat
import org.move.lang.core.psi.MoveNamedElement
import org.move.lang.core.psi.MovePat
import org.move.lang.core.psi.MoveVisitor

val MovePat.boundElements: List<MoveNamedElement>
    get() {
        val elements = mutableListOf<MoveNamedElement>()
        accept(object : MoveVisitor() {
            override fun visitBindingPat(o: MoveBindingPat) {
                elements.add(o)
            }
        })
        return elements
    }