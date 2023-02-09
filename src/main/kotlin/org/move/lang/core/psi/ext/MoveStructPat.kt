package org.move.lang.core.psi.ext

import org.move.lang.core.psi.MvStructPat
import org.move.lang.core.psi.MvStructPatField

val MvStructPat.fields: List<MvStructPatField>
    get() =
        structPatFieldsBlock.structPatFieldList

val MvStructPat.fieldNames: List<String>
    get() =
        fields.map { it.referenceName }

//fun MvStructPat.ty(): Ty {
//    val struct = this.path.reference?.resolve() as? MvStruct ?: return TyUnknown
//    // TODO: if one or more type arguments
//    if (struct.typeParameters.isNotEmpty()) return TyUnknown
//    return
////    return instantiateItemTy(struct, InferenceContext(this.isMsl()))
//}
