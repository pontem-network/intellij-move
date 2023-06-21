package org.move.lang.core.types.infer

import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.*
import org.move.lang.core.types.ty.*

//fun collectBindings(pattern: MvPat, inferredTy: Ty, parentCtx: InferenceContext) {
//    fun bind(pat: MvPat, ty: Ty) {
//        when (pat) {
//            is MvBindingPat -> {
//                parentCtx.patTypes[pat] = ty
////                parentCtx.bindingTypes[pat] = ty
//            }
//            is MvTuplePat -> {
//                if (ty is TyTuple && pat.patList.size == ty.types.size) {
//                    pat.patList.zip(ty.types)
//                        .forEach { (pat, ty) -> bind(pat, ty) }
//                } else {
//                    pat.patList.map { bind(it, TyUnknown) }
//                }
//            }
//            is MvStructPat -> {
//                val patTy = inferPatTy(pat, parentCtx, ty)
//                when (patTy) {
//                    is TyStruct -> {
//                        when {
//                            ty is TyUnknown -> pat.patFields.map {
//                                it.pat?.let { pat -> bind(pat, TyUnknown) }
//                            }
//                            ty is TyStruct && pat.patFields.size == ty.fieldTys.size -> {
//                                for (field in pat.patFields) {
//                                    val fieldTy = ty.fieldTy(field.referenceName)
//                                    field.pat?.let { bind(it, fieldTy) }
//                                }
//                            }
//                        }
//                    }
//                    is TyUnknown -> {
//                        pat.patFields.map {
//                            it.pat?.let { pat -> bind(pat, TyUnknown) }
//                        }
//                    }
//                    else -> error("unreachable with type ${patTy.fullname()}")
//                }
//                if (ty is TyStruct && pat.patFields.size == ty.fieldTys.size) {
//                    for (field in pat.patFields) {
//                        val fieldTy = ty.fieldTy(field.referenceName)
//                        field.pat?.let { bind(it, fieldTy) }
//                    }
//                } else {
//                    pat.patFields.map {
//                        it.pat?.let { pat -> bind(pat, TyUnknown) }
//                    }
//                }
//            }
//        }
//    }
//    bind(pattern, inferredTy)
//}

fun MvPat.anonymousTyVar(): Ty {
    return when (this) {
        is MvBindingPat -> TyInfer.TyVar()
        is MvTuplePat -> TyTuple(this.patList.map { TyInfer.TyVar() })
        else -> TyUnknown
    }
}

fun MvPat.extractBindings(fctx: TypeInferenceWalker, ty: Ty) {
    when (this) {
        is MvBindingPat -> {
            fctx.ctx.writePatTy(this, ty)
        }
        is MvStructPat -> {
            val structItem = this.structItem ?: (ty as? TyStruct)?.item ?: return
            val (patTy, _) = fctx.ctx.instantiatePath<TyStruct>(this.path, structItem) ?: return
            if (!isCompatible(ty, patTy, fctx.msl)) {
                fctx.reportTypeError(TypeError.InvalidUnpacking(this, ty))
            }
            val structFields = structItem.fields.associateBy { it.name }
            for (patField in this.patFields) {
                val kind = patField.kind
                val fieldType = structFields[kind.fieldName]
                    ?.type
                    ?.loweredType(fctx.msl)
                    ?.substituteOrUnknown(ty.typeParameterValues)
                    ?: TyUnknown
                when (kind) {
                    is PatFieldKind.Full -> kind.pat.extractBindings(fctx, fieldType)
                    is PatFieldKind.Shorthand -> kind.binding.extractBindings(fctx, fieldType)
                }
            }
        }
        is MvTuplePat -> {
            if (patList.size == 1 && ty !is TyTuple) {
                // let (a) = 1;
                // let (a,) = 1;
                patList.single().extractBindings(fctx, ty)
                return
            }
            val patTy = TyTuple.unknown(patList.size)
            val expectedTypes = if (!isCompatible(ty, patTy, fctx.msl)) {
                fctx.reportTypeError(TypeError.InvalidUnpacking(this, ty))
                emptyList()
            } else {
                (ty as? TyTuple)?.types.orEmpty()
            }
            for ((idx, p) in patList.withIndex()) {
                val patType = expectedTypes.getOrNull(idx) ?: TyUnknown
                p.extractBindings(fctx, patType)
            }
        }
    }
}

//fun inferPatTy(pat: MvPat, parentCtx: InferenceContext, expectedTy: Ty? = null): Ty {
//    val existingTy = parentCtx.patTypes[pat]
//    if (existingTy != null) {
//        return existingTy
//    }
//    val patTy = when (pat) {
//        is MvStructPat -> inferStructPatTy(pat, parentCtx, expectedTy)
//        is MvTuplePat -> {
//            val tupleTy = TyTuple(pat.patList.map { TyUnknown })
//            if (expectedTy != null) {
//                if (isCompatible(expectedTy, tupleTy)) {
//                    if (expectedTy is TyTuple) {
//                        val itemTys = pat.patList.mapIndexed { i, itemPat ->
//                            inferPatTy(
//                                itemPat,
//                                parentCtx,
//                                expectedTy.types[i]
//                            )
//                        }
//                        return TyTuple(itemTys)
//                    }
//                } else {
//                    parentCtx.typeErrors.add(TypeError.InvalidUnpacking(pat, expectedTy))
//                }
//            }
//            TyUnknown
//        }
//        is MvBindingPat -> {
//            val ty = expectedTy ?: TyUnknown
////            parentCtx.bindingTypes[pat] = ty
//            parentCtx.patTypes[pat] = ty
//            ty
//        }
//        else -> TyUnknown
//    }
//    parentCtx.writePatTy(pat, patTy)
//    return patTy
//}

//fun inferStructPatTy(structPat: MvStructPat, parentCtx: InferenceContext, expectedTy: Ty?): Ty {
//    val path = structPat.path
//    val structItem = structPat.structItem ?: return TyUnknown
//
//    val structTy = structItem.outerItemContext(parentCtx.msl).getStructItemTy(structItem)
//        ?: return TyUnknown
//
//    val inferenceCtx = InferenceContext(parentCtx.msl, parentCtx.itemContext)
//    // find all types passed as explicit type parameters, create constraints with those
//    if (path.typeArguments.isNotEmpty()) {
//        if (path.typeArguments.size != structTy.typeVars.size) return TyUnknown
//        for ((typeVar, typeArg) in structTy.typeVars.zip(path.typeArguments)) {
//            val typeArgTy = parentCtx.getTypeTy(typeArg.type)
//
//            // check compat for abilities
//            val compat = isCompatibleAbilities(typeVar, typeArgTy, path.isMsl())
//            val isCompat = when (compat) {
//                is Compat.AbilitiesMismatch -> {
//                    parentCtx.typeErrors.add(
//                        TypeError.AbilitiesMismatch(
//                            typeArg,
//                            typeArgTy,
//                            compat.abilities
//                        )
//                    )
//                    false
//                }
//
//                else -> true
//            }
//            inferenceCtx.registerEquateObligation(typeVar, if (isCompat) typeArgTy else TyUnknown)
//        }
//    }
//    if (expectedTy != null) {
//        if (isCompatible(expectedTy, structTy)) {
//            inferenceCtx.registerEquateObligation(structTy, expectedTy)
//        } else {
//            parentCtx.typeErrors.add(TypeError.InvalidUnpacking(structPat, expectedTy))
//        }
//    }
//    inferenceCtx.processConstraints()
//    parentCtx.resolveTyVarsFromContext(inferenceCtx)
//    return inferenceCtx.resolveTy(structTy)
//}

//fun inferBindingPatTy(bindingPat: MvBindingPat, parentCtx: InferenceContext): Ty {
//    val owner = bindingPat.owner
//    return when (owner) {
//        is MvFunctionParameter -> {
//            owner.type
//                ?.let { parentCtx.getTypeTy(it) }
//                ?: TyUnknown
//        }
//        is MvLetStmt -> {
//            val pat = owner.pat ?: return TyUnknown
//            val explicitType = owner.typeAnnotation?.type
//            if (explicitType != null) {
//                val explicitTy = parentCtx.getTypeTy(explicitType)
//                collectBindings(pat, explicitTy, parentCtx)
//                return parentCtx.bindingTypes[bindingPat] ?: TyUnknown
//            }
//            val inferredTy = owner.initializer?.expr?.let { inferExprTyOld(it, parentCtx) } ?: TyUnknown
//            collectBindings(pat, inferredTy, parentCtx)
//            return parentCtx.bindingTypes[bindingPat] ?: TyUnknown
//        }
//        is MvSchemaFieldStmt -> owner.annotationTy(parentCtx)
//        else -> TyUnknown
//    }
//}
