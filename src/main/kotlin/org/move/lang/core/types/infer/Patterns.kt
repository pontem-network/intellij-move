package org.move.lang.core.types.infer

import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.*
import org.move.lang.core.resolve.ref.resolvePatBindingWithExpectedType
import org.move.lang.core.resolve.scopeEntry.singleItemOrNull
import org.move.lang.core.types.infer.RsBindingModeKind.BindByReference
import org.move.lang.core.types.infer.RsBindingModeKind.BindByValue
import org.move.lang.core.types.ty.*
import org.move.lang.core.types.ty.Mutability.IMMUTABLE

fun MvPat.collectBindings(fcx: TypePsiWalker, ty: Ty, defBm: RsBindingModeKind = BindByValue) {
    val msl = this.isMsl()
    when (this) {
        is MvPatWild -> fcx.ctx.writePatTy(this, ty)
        is MvPatConst -> {
            // fills resolved paths
            val inferred = fcx.inferExprType(this.pathExpr)
            val item = fcx.ctx.resolvedPaths[this.pathExpr.path]?.singleOrNull { it.isVisible }?.element
            val pat_ty = when (item) {
                // copied from intellij-rust, don't know what it's about
                is MvConst -> ty
                else -> ty.stripReferences(defBm).first
            }
            fcx.coerceTypes(pathExpr, inferred, pat_ty)
            fcx.writePatTy(this, pat_ty)
        }
        is MvPatBinding -> {
            val resolveVariants = resolvePatBindingWithExpectedType(this, expectedType = ty)
            // todo: check visibility?
            val item = resolveVariants.singleItemOrNull()
            fcx.ctx.resolvedBindings[this] = item

            val bindingType =
                if (item is MvEnumVariant) {
                    ty.stripReferences(defBm).first
                } else {
                    ty.applyBm(defBm, msl)
                }
            fcx.writePatTy(this, bindingType)
        }
        is MvPatStruct -> {
            val (expected, patBm) = ty.stripReferences(defBm)
            fcx.ctx.writePatTy(this, expected)

            val item =
                fcx.resolvePathCached(this.path, expected) as? MvFieldsOwner
                    ?: (expected as? TyAdt)?.item as? MvStruct

            if (item is MvStruct) {
                val patTy = fcx.instantiatePath<TyAdt>(this.path, item) ?: return
                if (!isCompatible(expected, patTy, fcx.msl)) {
                    fcx.reportTypeError(TypeError.InvalidUnpacking(this, ty))
                }
            }

            val namedFields = item?.namedFields?.associateBy { it.name } ?: emptyMap()
            val tyAdtSubst = if (ty is TyAdt) ty.substitution else emptySubstitution
            for (fieldPat in this.patFieldList) {
                val kind = fieldPat.kind
                // wil have TyUnknown on unresolved item
                val namedField = namedFields[kind.fieldName]
                val fieldType = namedField
                    ?.type
                    ?.loweredType(fcx.msl)
                    ?.substituteOrUnknown(tyAdtSubst)
                    ?: TyUnknown

                when (kind) {
                    is PatFieldKind.Full -> {
                        kind.pat.collectBindings(fcx, fieldType, patBm)
                        fcx.ctx.writeFieldPatTy(fieldPat, fieldType)
                    }
                    is PatFieldKind.Shorthand -> {
                        fcx.ctx.resolvedBindings[kind.binding] = namedField
                        fcx.ctx.writeFieldPatTy(fieldPat, fieldType.applyBm(patBm, msl))
                    }
                }
            }
        }
        is MvPatTupleStruct -> {
            val (expected, patBm) = ty.stripReferences(defBm)
            fcx.ctx.writePatTy(this, expected)
            val item =
                fcx.resolvePathCached(this.path, expected) as? MvFieldsOwner
                    ?: (expected as? TyAdt)?.item as? MvStruct
            if (item == null) {
                patList.forEach { it.collectBindings(fcx, TyUnknown) }
                return
            }
            val tupleFields = item.positionalFields
            val subst = if (expected is TyAdt) expected.substitution else emptySubstitution
            inferTupleFieldsTypes(
                fcx,
                patList,
                patBm,
                tupleFields.size
            ) { indx ->
                tupleFields
                    .getOrNull(indx)
                    ?.type
                    ?.loweredType(fcx.msl)
                    ?.substituteOrUnknown(subst)
                    ?: TyUnknown

            }
        }
        is MvPatTuple -> {
            if (patList.size == 1 && ty !is TyTuple) {
                // let (a) = 1;
                // let (a,) = 1;
                patList.single().collectBindings(fcx, ty)
                return
            }

            // try for invalid unpacking
            if (!isCompatible(ty, TyTuple.unknown(patList.size), fcx.msl)) {
                fcx.reportTypeError(TypeError.InvalidUnpacking(this, ty))
            }

            val types = (ty as? TyTuple)?.types.orEmpty()
            inferTupleFieldsTypes(
                fcx,
                patList,
                BindByValue,
                types.size
            ) { types.getOrElse(it) { TyUnknown } }
        }
    }
}

private fun inferTupleFieldsTypes(
    fcx: TypePsiWalker,
    patList: List<MvPat>,
    bm: RsBindingModeKind,
    tupleSize: Int,
    type: (Int) -> Ty,
) {
    // In correct code, tuple or tuple struct patterns contain only one `..` pattern.
    // But it's pretty simple to support type inference for cases with multiple `..` patterns like `let (x, .., y, .., z) = tuple`
    // just ignoring all binding between first and last `..` patterns
    var firstPatRestIndex = Int.MAX_VALUE
    var lastPatRestIndex = -1
    for ((idx, pat) in patList.withIndex()) {
        if (pat is MvPatRest) {
            firstPatRestIndex = minOf(firstPatRestIndex, idx)
            lastPatRestIndex = maxOf(lastPatRestIndex, idx)
        }
    }

    for ((idx, pat) in patList.withIndex()) {
        val fieldTy = when {
            idx < firstPatRestIndex -> type(idx)
            idx > lastPatRestIndex -> type(tupleSize - (patList.size - idx))
            else -> TyUnknown
        }
        pat.collectBindings(fcx, fieldTy, bm)
    }
}

private fun Ty.applyBm(defBm: RsBindingModeKind, msl: Boolean): Ty {
    return if (defBm is BindByReference) TyReference(this, defBm.mutability, msl) else this
}

private fun Ty.stripReferences(defBm: RsBindingModeKind): Pair<Ty, RsBindingModeKind> {
    var bm = defBm
    var ty = this
    while (ty is TyReference) {
        bm = when (bm) {
            is BindByValue -> BindByReference(ty.mutability)
            is BindByReference -> BindByReference(
                if (bm.mutability == IMMUTABLE) IMMUTABLE else ty.mutability
            )
        }
        ty = ty.referenced
    }
    return ty to bm
}

fun MvPat.anonymousTyVar(): Ty {
    return when (this) {
        is MvPatBinding -> TyInfer.TyVar()
        is MvPatTuple -> TyTuple(this.patList.map { TyInfer.TyVar() })
        else -> TyUnknown
    }
}

sealed class RsBindingModeKind {
    data object BindByValue: RsBindingModeKind()
    class BindByReference(val mutability: Mutability): RsBindingModeKind()
}
