package org.move.lang.core.types.ty

import org.move.lang.core.psi.MvSchema
import org.move.lang.core.psi.tyTypeParams
import org.move.lang.core.psi.tyTypeParamsSubst
import org.move.lang.core.types.infer.Substitution
import org.move.lang.core.types.infer.TypeFolder
import org.move.lang.core.types.infer.mergeFlags

data class TySchema(
    val item: MvSchema,
    val substitution: Substitution,
    val typeArguments: List<Ty>
) : Ty(mergeFlags(substitution.valueTys) or mergeFlags(typeArguments)) {

    override fun abilities(): Set<Ability> = Ability.all()

    override fun deepFoldWith(folder: TypeFolder): Ty {
        return TySchema(
            item,
            substitution.foldWith(folder),
            typeArguments.map { it.foldWith(folder) }
        )
    }

    companion object {
        fun valueOf(schema: MvSchema): TySchema {
            val typeParameters = schema.tyTypeParamsSubst
            return TySchema(
                schema,
                typeParameters,
                schema.tyTypeParams
            )
        }
    }
}
