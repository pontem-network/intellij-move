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
            val inferred = fcx.inferType(pathExpr)
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
                fcx.resolvePathElement(this, expected) as? MvFieldsOwner
                    ?: (expected as? TyAdt)?.item as? MvStruct
                    ?: return

            if (item is MvTypeParametersOwner) {
                val (patTy, _) = fcx.ctx.instantiateMethodOrPath<TyAdt>(this.path, item) ?: return
                if (!isCompatible(expected, patTy, fcx.msl)) {
                    fcx.reportTypeError(TypeError.InvalidUnpacking(this, ty))
                }
            }

            val structFields = item.fields.associateBy { it.name }
            for (fieldPat in this.patFieldList) {
                val kind = fieldPat.kind
                val fieldType = structFields[kind.fieldName]
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
                        fcx.ctx.writeFieldPatTy(fieldPat, fieldType.applyBm(patBm, msl))
                    }
                }
            }
        }
        is MvPatTuple -> {
            if (patList.size == 1 && ty !is TyTuple) {
                // let (a) = 1;
                // let (a,) = 1;
                patList.single().extractBindings(fcx, ty)
                return
            }
            val patTy = TyTuple.unknown(patList.size)
            val expectedTypes = if (!isCompatible(ty, patTy, fcx.msl)) {
                fcx.reportTypeError(TypeError.InvalidUnpacking(this, ty))
                emptyList()
            } else {
                (ty as? TyTuple)?.types.orEmpty()
            }
            for ((idx, p) in patList.withIndex()) {
                val patType = expectedTypes.getOrNull(idx) ?: TyUnknown
                p.extractBindings(fcx, patType)
            }
        }
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
