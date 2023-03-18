package org.move.lang.core.psi

import org.move.lang.core.psi.ext.isMsl
import org.move.lang.core.types.infer.ItemContext
import org.move.lang.core.types.infer.inferItemTypeTy
import org.move.lang.core.types.infer.itemContext
import org.move.lang.core.types.ty.Ty
import org.move.lang.core.types.ty.TyUnknown

val MvTypeAnnotationOwner.type: MvType? get() = this.typeAnnotation?.type

// Not cached, use only when there's no InferenceContext available
fun MvTypeAnnotationOwner.typeTy(itemContext: ItemContext): Ty {
    val type = this.type ?: return TyUnknown
    return inferItemTypeTy(type, itemContext)
}

interface MvTypeAnnotationOwner : MvElement {

    val typeAnnotation: MvTypeAnnotation?
}
