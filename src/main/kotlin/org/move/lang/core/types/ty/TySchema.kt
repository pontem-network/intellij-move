package org.move.lang.core.types.ty

import org.move.lang.core.psi.MvSchema
import org.move.lang.core.types.infer.Substitution

data class TySchema(
    override val item: MvSchema,
    override val substitution: Substitution,
) : GenericTy(item, substitution, 0) {

    override fun abilities(): Set<Ability> = Ability.all()
}
