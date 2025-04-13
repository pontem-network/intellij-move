package org.move.lang.core.types.infer

import org.move.lang.core.types.ty.Ty

typealias TypeFlags = Int

const val HAS_TY_INFER_MASK: TypeFlags = 1
const val HAS_TY_TYPE_PARAMETER_MASK: TypeFlags = 2
const val HAS_TY_ADT_MASK: TypeFlags = 4
const val HAS_TY_UNKNOWN_MASK: TypeFlags = 8

fun mergeFlags(tys: Collection<Ty>): TypeFlags =
    tys.fold(0) { a, b -> a or b.flags }

data class HasTypeFlagVisitor(val mask: TypeFlags): TypeVisitor() {

    override fun visit(ty: Ty): Boolean = ty.flags.and(mask) != 0

    companion object {
        val HAS_TY_INFER_VISITOR = HasTypeFlagVisitor(HAS_TY_INFER_MASK)
        val HAS_TY_TYPE_PARAMETER_VISITOR = HasTypeFlagVisitor(HAS_TY_TYPE_PARAMETER_MASK)
        val HAS_TY_ADT_VISITOR = HasTypeFlagVisitor(HAS_TY_ADT_MASK)
        val HAS_TY_UNKNOWN_VISITOR = HasTypeFlagVisitor(HAS_TY_UNKNOWN_MASK)

        val NEEDS_SUBST = HasTypeFlagVisitor(HAS_TY_TYPE_PARAMETER_MASK)
    }
}
