package org.move.lang.core.psi.ext

import org.move.lang.core.psi.MvStruct
import org.move.lang.core.psi.MvStructPat
import org.move.lang.core.psi.MvStructPatField
import org.move.lang.core.psi.typeParameters
import org.move.lang.core.types.infer.instantiateItemTy
import org.move.lang.core.types.ty.Ty
import org.move.lang.core.types.ty.TyUnknown

val MvStructPat.fields: List<MvStructPatField>
    get() =
        structPatFieldsBlock.structPatFieldList

val MvStructPat.fieldNames: List<String>
    get() =
        fields.map { it.referenceName }

fun MvStructPat.ty(): Ty {
    val struct = this.path.reference?.resolve() as? MvStruct ?: return TyUnknown
    // TODO: if one or more type arguments
    if (struct.typeParameters.isNotEmpty()) return TyUnknown
    return instantiateItemTy(struct, this.isMsl())
}
