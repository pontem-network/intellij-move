package org.move.lang.core.psi

interface MoveDeclaringElement: MoveElement {

    val boundElements: Collection<MoveNamedElement>
}