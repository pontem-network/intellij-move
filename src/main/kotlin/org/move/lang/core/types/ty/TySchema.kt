package org.move.lang.core.types.ty

import org.move.lang.core.psi.MvSchema
import org.move.lang.core.types.infer.Substitution
import org.move.lang.core.types.infer.TypeFolder
import org.move.lang.core.types.infer.mergeFlags

data class TySchema(
    override val item: MvSchema,
    override val substitution: Substitution,
    val typeArguments: List<Ty>
) : GenericTy(item, substitution, mergeFlags(typeArguments)) {

    override fun abilities(): Set<Ability> = Ability.all()

    override fun deepFoldWith(folder: TypeFolder): Ty {
        return TySchema(
            item,
            substitution.foldValues(folder),
            typeArguments.map { it.foldWith(folder) }
        )
    }
}
