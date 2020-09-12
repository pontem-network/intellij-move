package org.move.lang.core.psi

interface MoveTypeAnnotated {
    val typeAnnotation: MoveTypeAnnotation?
}

val MoveTypeAnnotated.type: MoveType? get() = typeAnnotation?.type