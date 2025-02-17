package org.move.lang.core.types.infer

import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.*
import org.move.lang.core.psi.ext.RsBindingModeKind.BindByReference
import org.move.lang.core.psi.ext.RsBindingModeKind.BindByValue
import org.move.lang.core.resolve2.ref.resolvePatBindingRaw
import org.move.lang.core.types.ty.*
import org.move.lang.core.types.ty.Mutability.IMMUTABLE

fun MvPat.extractBindings(fcx: TypeInferenceWalker, ty: Ty, defBm: RsBindingModeKind = BindByValue) {
    val msl = this.isMsl()
    when (this) {
        is MvPatWild -> fcx.writePatTy(this, ty)
        is MvPatConst -> {
            // fills resolved paths
            val inferred = fcx.inferExprType(pathExpr)
            val resolvedItem = fcx.getResolvedPath(pathExpr.path).singleOrNull { it.isVisible }?.element
            val expected = when (resolvedItem) {
                // copied from intellij-rust, don't know what it's about
                is MvConst -> ty
                else -> ty.stripReferences(defBm).first
            }
            fcx.coerce(pathExpr, inferred, expected)
            fcx.writePatTy(this, expected)
        }
        is MvPatBinding -> {
            val resolveVariants = resolvePatBindingRaw(this, expectedType = ty)
            // todo: check visibility?
            val item = resolveVariants.singleOrNull()?.element
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

            if (item is MvGenericDeclaration) {
                val patTy = fcx.instantiatePath<TyAdt>(this.path, item) ?: return
                if (!isCompatible(expected, patTy, fcx.msl)) {
                    fcx.reportTypeError(TypeError.InvalidUnpacking(this, ty))
                }
            }

            val namedFields = item?.namedFields?.associateBy { it.name } ?: emptyMap()
            for (fieldPat in this.patFieldList) {
                val kind = fieldPat.kind
                // wil have TyUnknown on unresolved item
                val namedField = namedFields[kind.fieldName]
                val fieldType = namedField
                    ?.type
                    ?.loweredType(fcx.msl)
                    ?.substituteOrUnknown(ty.typeParameterValues)
                    ?: TyUnknown

                when (kind) {
                    is PatFieldKind.Full -> {
                        kind.pat.extractBindings(fcx, fieldType, patBm)
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
                patList.forEach { it.extractBindings(fcx, TyUnknown) }
                return
            }
            val tupleFields = item.positionalFields
            inferTupleFieldsTypes(fcx, patList, patBm, tupleFields.size) { indx ->
                tupleFields
                    .getOrNull(indx)
                    ?.type
                    ?.loweredType(fcx.msl)
                    ?.substituteOrUnknown(ty.typeParameterValues)
                    ?: TyUnknown

            }
        }
        is MvPatTuple -> {
            if (patList.size == 1 && ty !is TyTuple) {
                // let (a) = 1;
                // let (a,) = 1;
                patList.single().extractBindings(fcx, ty)
                return
            }

            // try for invalid unpacking
            if (!isCompatible(ty, TyTuple.unknown(patList.size), fcx.msl)) {
                fcx.reportTypeError(TypeError.InvalidUnpacking(this, ty))
            }

            val types = (ty as? TyTuple)?.types.orEmpty()
            inferTupleFieldsTypes(fcx, patList, BindByValue, types.size) { types.getOrElse(it) { TyUnknown } }
        }
    }
}

private fun inferTupleFieldsTypes(
    fcx: TypeInferenceWalker,
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
    for ((index, pat) in patList.withIndex()) {
        if (pat is MvPatRest) {
            firstPatRestIndex = minOf(firstPatRestIndex, index)
            lastPatRestIndex = maxOf(lastPatRestIndex, index)
        }
    }

    for ((idx, p) in patList.withIndex()) {
        val fieldType = when {
            idx < firstPatRestIndex -> type(idx)
            idx > lastPatRestIndex -> type(tupleSize - (patList.size - idx))
            else -> TyUnknown
        }
        p.extractBindings(fcx, fieldType, bm)
    }
}

private fun Ty.applyBm(defBm: RsBindingModeKind, msl: Boolean): Ty =
    if (defBm is BindByReference) TyReference(this, defBm.mutability, msl) else this

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
