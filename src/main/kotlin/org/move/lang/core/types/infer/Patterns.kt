package org.move.lang.core.types.infer

import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.*
import org.move.lang.core.resolve.pickFirstResolveVariant
import org.move.lang.core.resolve.processAll
import org.move.lang.core.resolve.ref.TYPES
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

fun MvPat.collectBindings(fctx: TypeInferenceWalker, ty: Ty) {
    when (this) {
        is MvBindingPat -> fctx.ctx.writePatTy(this, ty)
        is MvStructPat -> {
            fctx.ctx.writePatTy(this, ty)
            val path = path
            val item = when {
                this.parent is MvMatchArm && path.path == null -> {
                    // if we're inside match arm and no qualifier,
                    // StructPat can only be a enum variant, resolve through type.
                    // Otherwise there's a resolution cycle when we call the .resolve() method

                    // NOTE: I think it can be replaced moving path resolution to the inference,
                    // like it's done in intellij-rust
                    val referenceName = path.referenceName ?: return
                    val enumItem = (ty as? TyAdt)?.item as? MvEnum ?: return
                    pickFirstResolveVariant(referenceName) {
                        it.processAll(TYPES, enumItem.variants)
                    } as? MvFieldsOwner
                }
                else -> {
                    path.reference?.resolveFollowingAliases() as? MvFieldsOwner
                        ?: ((ty as? TyAdt)?.item as? MvStruct)
                }
            } ?: return
//            val item = path.reference?.resolveFollowingAliases() as? MvFieldsOwner
//                ?: ((ty as? TyAdt)?.item as? MvStruct)
//                ?: return

            if (item is MvTypeParametersOwner) {
                val (patTy, _) = fctx.ctx.instantiateMethodOrPath<TyAdt>(this.path, item) ?: return
                if (!isCompatible(ty.derefIfNeeded(), patTy, fctx.msl)) {
                    fctx.reportTypeError(TypeError.InvalidUnpacking(this, ty))
                }
            }

            val structFields = item.fields.associateBy { it.name }
            for (fieldPat in this.fieldPatList) {
                val kind = fieldPat.kind
                val fieldType = structFields[kind.fieldName]
                    ?.type
                    ?.loweredType(fctx.msl)
                    ?.substituteOrUnknown(ty.typeParameterValues)
                    ?.let { if (ty is TyReference) ty.transferReference(it) else it }
                    ?: TyUnknown

                when (kind) {
                    is PatFieldKind.Full -> kind.pat.collectBindings(fctx, fieldType)
                    is PatFieldKind.Shorthand -> kind.binding.collectBindings(fctx, fieldType)
                }
                fctx.ctx.writeFieldPatTy(fieldPat, fieldType)
            }
        }
        is MvTuplePat -> {
            if (patList.size == 1 && ty !is TyTuple) {
                // let (a) = 1;
                // let (a,) = 1;
                patList.single().collectBindings(fctx, ty)
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
                p.collectBindings(fctx, patType)
            }
        }
    }
}

//private fun MvBindingPat.inferType(expected: Ty, /*defBm: RsBindingModeKind*/): Ty {
////    val bm = run {
////        val bm = kind
////        if (bm is BindByValue && bm.mutability == IMMUTABLE) {
////            defBm
////        } else {
////            bm
////        }
////    }
////    return if (bm is BindByReference) TyReference(expected, bm.mutability) else expected
//    return expected
//}


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
//            val compat = isCompatibleAbilities(typeVar, typeArgTy, path.isMslLegacy())
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
