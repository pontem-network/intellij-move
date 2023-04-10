package org.move.lang.core.types.infer

import com.intellij.openapi.progress.ProgressManager
import com.intellij.psi.PsiElement
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.*
import org.move.lang.core.types.ty.*
import org.move.stdext.RsResult

class TypeInferenceWalker(
    val ctx: InferenceContext,
    val msl: Boolean,
    private val returnTy: Ty
) {

    private val fulfillmentContext: FulfillmentContext = ctx.fulfill

    fun extractParameterBindings(owner: MvInferenceContextOwner) {
        val bindings = when (owner) {
            is MvFunction -> owner.allParamsAsBindings
            is MvSpecFunction -> owner.allParamsAsBindings
            is MvItemSpec -> owner.funcItem?.allParamsAsBindings.orEmpty()
            is MvSchema -> owner.fieldStmts.map { it.bindingPat }
            else -> emptyList()
        }
        for (binding in bindings) {
            this.ctx.writePatTy(binding, inferBindingPatTy(binding, this.ctx))
        }
    }

    fun inferFnBody(block: MvCodeBlock): Ty =
        block.inferTypeCoercableTo(returnTy)

    private fun MvCodeBlock.inferTypeCoercableTo(expected: Ty): Ty =
        inferCodeBlockTy(this, Expectation.ExpectHasType(expected), true)

    fun inferCodeBlockTy(block: MvCodeBlock, expected: Expectation, coerce: Boolean = false): Ty {
        for (stmt in block.stmtList) {
            processStatement(stmt)
//            blockCtx.processConstraints()
//            blockCtx.resolveTyVarsFromContext(blockCtx)
        }
        val tailExpr = block.expr

        val type = if (coerce && expected is Expectation.ExpectHasType) {
            tailExpr?.inferTypeCoercableTo(expected.ty)
        } else {
            tailExpr?.let { inferExprTy(it, ctx, expected) }
//            tailExpr?.inferType(expected)
        } ?: TyUnit
//        return if (isDiverging) TyNever else type
        return type

//        if (tailExpr == null) {
//            val expectedTy = expectation.onlyHasTy(ctx)
//            if (expectedTy != null && expectedTy !is TyUnit) {
//                reportTypeMismatch(block.rightBrace ?: block, expectedTy, TyUnit)
////                blockCtx.typeErrors.add(
////                    TypeError.TypeMismatch(
////                        block.rightBrace ?: block,
////                        expectedTy,
////                        TyUnit
////                    )
////                )
//                return TyUnknown
//            }
//            return TyUnit
//        } else {
//            val tailExprTy = inferExprTy(tailExpr, ctx, expectation)
////            blockCtx.processConstraints()
////            blockCtx.resolveTyVarsFromContext(blockCtx)
//            return tailExprTy
//        }
    }

    fun inferSpecBlock(block: MvItemSpecBlock, expectedTy: Ty? = null): Ty {
        for (stmt in block.stmtList) {
            processStatement(stmt)
//            ctx.processConstraints()
//            ctx.resolveTyVarsFromContext(ctx)
        }
        val tailExpr = block.expr
        if (tailExpr == null) {
            if (expectedTy != null && expectedTy !is TyUnit) {
                reportTypeMismatch(block.rBrace ?: block, expectedTy, TyUnit)
//                blockCtx.typeErrors.add(
//                    TypeError.TypeMismatch(
//                        block.rightBrace ?: block,
//                        expectedTy,
//                        TyUnit
//                    )
//                )
                return TyUnknown
            }
            return TyUnit
        } else {
            val tailExprTy = inferExprTy(tailExpr, ctx)
//            blockCtx.processConstraints()
//            blockCtx.resolveTyVarsFromContext(blockCtx)
            return tailExprTy
        }
    }

    private fun resolveTypeVarsWithObligations(ty: Ty): Ty {
        if (!ty.hasTyInfer) return ty
        val tyRes = ctx.resolveTypeVarsIfPossible(ty)
        if (!tyRes.hasTyInfer) return tyRes
        selectObligationsWherePossible()
        return ctx.resolveTypeVarsIfPossible(tyRes)
    }

    private fun selectObligationsWherePossible() {
        fulfillmentContext.selectWherePossible()
    }

    private fun processStatement(stmt: MvStmt) {
        when (stmt) {
            is MvLetStmt -> {
                val explicitTy = stmt.typeAnnotation?.type?.let { ctx.getTypeTy(it) }
                val initializerTy = stmt.initializer?.expr?.let { inferExprTy(it, ctx, explicitTy) }
                val pat = stmt.pat ?: return
//                val patTy = inferPatTy(pat, ctx, explicitTy ?: initializerTy)
                val patTy = explicitTy ?: resolveTypeVarsWithObligations(initializerTy ?: TyUnknown)
                collectBindings(pat, patTy, ctx)
            }
            is MvExprStmt -> inferExprTy(stmt.expr, ctx)
            is MvSpecExprStmt -> inferExprTy(stmt.expr, ctx)
            is MvSchemaFieldStmt -> {
            }
        }
    }

    fun inferExprTy(
        expr: MvExpr,
        parentCtx: InferenceContext,
        expected: Expectation = Expectation.NoExpectation
    ): Ty {
        ProgressManager.checkCanceled()
        if (parentCtx.isTypeInferred(expr)) error("Trying to infer expression type twice")

        expected.tyAsNullable(ctx)?.let {
            when (expr) {
                is MvRefExpr, is MvDotExpr, is MvCallExpr -> ctx.writeExprExpectedTy(expr, it)
            }
        }

        val itemContext =
            expr.itemContextOwner?.itemContext(parentCtx.msl) ?: expr.project.itemContext(parentCtx.msl)
        var exprTy = when (expr) {
            is MvRefExpr -> inferRefExprTy(expr, parentCtx)
            is MvBorrowExpr -> inferBorrowExprTy(expr, parentCtx, expected)
            is MvCallExpr -> {
                inferCallExprTy(expr, parentCtx, expected)
//                val funcTy = inferCallExprTy(expr, parentCtx, expected) as? TyFunction
//                funcTy?.retType ?: TyUnknown
            }
            is MvMacroCallExpr -> {
                inferMacroCallExprTy(expr)
//                for (argumentExpr in expr.callArgumentExprs) {
//                    inferExprTy(argumentExpr, parentCtx)
//                }
//                TyUnknown
            }
            is MvStructLitExpr -> inferStructLitExprTy(expr, parentCtx, expected)
            is MvVectorLitExpr -> inferVectorLitExpr(expr, parentCtx)

            is MvDotExpr -> inferDotExprTy(expr, parentCtx)
            is MvDerefExpr -> inferDerefExprTy(expr, parentCtx)
            is MvLitExpr -> inferLitExprTy(expr, parentCtx)
            is MvTupleLitExpr -> inferTupleLitExprTy(expr, parentCtx)

            is MvMoveExpr -> expr.expr?.let { inferExprTy(it, parentCtx) } ?: TyUnknown
            is MvCopyExpr -> expr.expr?.let { inferExprTy(it, parentCtx) } ?: TyUnknown

            is MvItemSpecBlockExpr -> expr.specBlock?.let { inferSpecBlock(it) } ?: TyUnknown

            is MvCastExpr -> {
                inferExprTy(expr.expr, parentCtx)
                parentCtx.getTypeTy(expr.type)
            }
            is MvParensExpr -> expr.expr?.let { inferExprTy(it, parentCtx) } ?: TyUnknown

            is MvBinaryExpr -> inferBinaryExprTy(expr, parentCtx)
            is MvBangExpr -> {
                expr.expr?.let { inferExprTy(it, parentCtx, Expectation.maybeHasType(TyBool)) }
                TyBool
            }

            is MvIfExpr -> inferIfExprTy(expr, parentCtx, Expectation.maybeHasType(TyBool))
            is MvWhileExpr -> inferWhileExpr(expr, parentCtx)
            is MvLoopExpr -> inferLoopExpr(expr, parentCtx)
            is MvReturnExpr -> {
                val expectedReturnTy = expr.containingFunction?.let {
                    (itemContext.getItemTy(it) as? TyFunction)?.retType
                }
                expr.expr?.let { inferExprTy(it, parentCtx, expectedReturnTy) }
                TyNever
            }
            is MvCodeBlockExpr -> inferCodeBlockTy(expr.codeBlock, expected)
            is MvAssignmentExpr -> {
                val lhsExprTy = inferExprTy(expr.expr, parentCtx)
                expr.initializer.expr?.let { inferExprTy(it, parentCtx, lhsExprTy) }
                TyUnit
            }
            else -> TyUnknown
        }
        if (expr.isMsl()) {
            exprTy = refineTypeForMsl(exprTy)
        }
//        if (exprTy is TyReference && expr.isMsl()) {
//            exprTy = exprTy.innermostTy()
//        }
//        if (exprTy is TyInteger && expr.isMsl()) {
//            exprTy = TyNum
//        }

//        if (expectedTy != null) {
//            ctx.combineTypes(expectedTy, exprTy)
////            parentCtx.registerEquateObligation(exprTy, expectedTy)
////            val compat = checkTysCompatible(expectedTy, exprTy, parentCtx.msl)
////            when (compat) {
////                is Compat.AbilitiesMismatch -> {
////                    parentCtx.typeErrors.add(TypeError.AbilitiesMismatch(expr, exprTy, compat.abilities))
////                }
////                is Compat.TypeMismatch -> {
////                    parentCtx.typeErrors.add(TypeError.TypeMismatch(expr, expectedTy, exprTy))
////                }
////                else -> parentCtx.registerEquateObligation(exprTy, expectedTy)
////            }
//        }

        parentCtx.writeExprTy(expr, exprTy)
        return exprTy
    }

    private fun MvExpr.inferTypeCoercableTo(expected: Ty): Ty {
        val inferred = inferExprTy(this, ctx, Expectation.maybeHasType(expected))
        return if (coerce(this, inferred, expected)) expected else inferred
    }

    private fun refineTypeForMsl(ty: Ty): Ty {
        var exprTy = ty
        if (ty is TyReference) {
            exprTy = ty.innermostTy()
        }
        if (exprTy is TyInteger) {
            exprTy = TyNum
        }
        return exprTy
    }

    private fun inferRefExprTy(refExpr: MvRefExpr, ctx: InferenceContext): Ty {
        val item = refExpr.path.reference?.resolveWithAliases()
        return when (item) {
            is MvBindingPat -> ctx.getPatType(item)
            is MvConst -> {
                val itemContext = item.outerItemContext(ctx.msl)
                itemContext.getConstTy(item)
            }
            else -> TyUnknown
        }
    }

    private fun inferBorrowExprTy(borrowExpr: MvBorrowExpr, ctx: InferenceContext, expected: Expectation): Ty {
        val innerExpr = borrowExpr.expr ?: return TyUnknown
        val expectedInnerTy = (expected.onlyHasTy(ctx) as? TyReference)?.referenced
        val hint = Expectation.maybeHasType(expectedInnerTy)

        val innerExprTy = inferExprTy(innerExpr, ctx, hint)
        val mutabilities = RefPermissions.valueOf(borrowExpr.isMut)
        return TyReference(innerExprTy, mutabilities, ctx.msl)
    }

//    private fun inferRefType(expr: MvExpr, expected: Expectation, mutable: Mutability): Ty {
//        val hint = expected.onlyHasTy(ctx)?.let {
//            val referenced = when (it) {
//                is TyReference -> it.referenced
//                is TyPointer -> it.referenced
//                else -> null
//            }
//            if (referenced != null) {
//                val isPlace = when (expr) {
//                    is RsPathExpr, is RsIndexExpr -> true
//                    is RsUnaryExpr -> expr.operatorType == UnaryOperator.DEREF
//                    is RsDotExpr -> expr.fieldLookup != null
//                    else -> false
//                }
//                if (isPlace) {
//                    ExpectHasType(referenced)
//                } else {
//                    Expectation.rvalueHint(referenced)
//                }
//            } else {
//                NoExpectation
//            }
//        } ?: NoExpectation
//
//        val ty = expr.inferType(hint, needs = Needs.maybeMutPlace(mutable))
//
//        return when (kind) {
//            BorrowKind.REF -> TyReference(ty, mutable) // TODO infer the actual lifetime
//            BorrowKind.RAW -> TyPointer(ty, mutable)
//        }
//    }

    private fun inferCallExprTy(
        callExpr: MvCallExpr,
        parentCtx: InferenceContext,
        expected: Expectation
    ): Ty {
//        if (parentCtx.callExprTypes.containsKey(callExpr)) error("Trying to infer call expression type twice")
//        val existingTy = parentCtx.callExprTypes[callExpr]
//        if (existingTy != null) {
//            return existingTy
//        }

        val path = callExpr.path
        val funcItem = path.reference?.resolveWithAliases() as? MvFunctionLike ?: return TyUnknown

        var funcTy = funcItem.outerItemContext(parentCtx.msl).getFunctionItemTy(funcItem)

        val inferenceCtx = InferenceContext(parentCtx.msl, parentCtx.itemContext)
        val callExprMsl = callExpr.isMsl()
        // find all types passed as explicit type parameters, create constraints with those
        if (path.typeArguments.isNotEmpty()) {
            if (path.typeArguments.size != funcTy.typeVars.size) return TyUnknown
            for ((typeVar, typeArg) in funcTy.typeVars.zip(path.typeArguments)) {
                val typeArgTy = parentCtx.getTypeTy(typeArg.type)

                // check compat for abilities
                val compat = isCompatibleAbilities(typeVar, typeArgTy, callExprMsl)
                val isCompat = when (compat) {
                    is Compat.AbilitiesMismatch -> {
                        parentCtx.typeErrors.add(
                            TypeError.AbilitiesMismatch(
                                typeArg,
                                typeArgTy,
                                compat.abilities
                            )
                        )
                        false
                    }

                    else -> true
                }
                inferenceCtx.registerEquateObligation(typeVar, if (isCompat) typeArgTy else TyUnknown)
            }
        }

        val expectedParamTypes =
            expectedInputsForExpectedOutput(expected, funcTy.retType, funcTy.paramTypes)

        inferenceCtx.processConstraints()
        funcTy = inferenceCtx.resolveTy(funcTy) as TyFunction

        inferArgumentTypes(funcTy.paramTypes, expectedParamTypes, callExpr.callArgumentExprs)

//        // find all types of passed expressions, create constraints with those
//        for ((i, argumentExpr) in callExpr.callArgumentExprs.withIndex()) {
//
//
////            val paramTy = funcTy.paramTypes.getOrNull(i) ?: break
//            val formalParamTy = funcTy.paramTypes.getOrNull(i) ?: TyUnknown
//            val expectedInputTy = expectedParamTypes.getOrNull(i) ?: formalParamTy
//
//            val expectation = Expectation.maybeHasType(expectedInputTy)
//            val inferredTy = inferExprTy(argumentExpr, parentCtx, expectation)
//            val coercedTy =
//                resolveTypeVarsWithObligations(expectation.onlyHasTy(ctx) ?: formalParamTy)
//            coerce(expr, inferredTy, coercedTy)
//
//            // get obligations
//            ctx.combineTypes(formalParamTy, coercedTy)
//
//////            val argumentExprTy = inferExprTy(argumentExpr, parentCtx, paramTy)
////            inferenceCtx.registerEquateObligation(paramTy, inferredTy)
//        }
//        if (expected != null) {
//            inferenceCtx.registerEquateObligation(funcTy.retType, expected)
//        }

//        inferenceCtx.processConstraints()
//        funcTy = inferenceCtx.resolveTy(funcTy) as TyFunction

        parentCtx.writeAcquiredTypes(callExpr, funcTy.acquiresTypes)
//        parentCtx.cacheCallExprTy(callExpr, funcTy)
        return funcTy.retType
    }

    private fun inferArgumentTypes(
        formalInputTys: List<Ty>,
        expectedInputTys: List<Ty>,
        argExprs: List<MvExpr>
    ) {
        // find all types of passed expressions, create constraints with those
        for ((i, argExpr) in argExprs.withIndex()) {

//            inferenceCtx.processConstraints()
//            funcTy = inferenceCtx.resolveTy(funcTy) as TyFunction

//            val paramTy = funcTy.paramTypes.getOrNull(i) ?: break
            val formalInputTy = formalInputTys.getOrNull(i) ?: TyUnknown
            val expectedInputTy = expectedInputTys.getOrNull(i) ?: formalInputTy

            val expectation = Expectation.maybeHasType(expectedInputTy)
            val inferredTy = inferExprTy(argExpr, ctx, expectation)
            val coercedTy =
                resolveTypeVarsWithObligations(expectation.onlyHasTy(ctx) ?: formalInputTy)
            coerce(argExpr, inferredTy, coercedTy)

            // retrieve obligations
            ctx.combineTypes(formalInputTy, coercedTy)

////            val argumentExprTy = inferExprTy(argumentExpr, parentCtx, paramTy)
//            inferenceCtx.registerEquateObligation(paramTy, inferredTy)
        }
//        if (expected != null) {
//            inferenceCtx.registerEquateObligation(funcTy.retType, expected)
//        }
    }

    fun inferMacroCallExprTy(macroExpr: MvMacroCallExpr): Ty {
        val ident = macroExpr.macroIdent.identifier
        if (ident.text == "assert") {
            val formalInputTys = listOf(TyBool, TyInteger.default())
            inferArgumentTypes(formalInputTys, emptyList(), macroExpr.callArgumentExprs)
        }
        return TyUnit
    }

    /**
     * Unifies the output type with the expected type early, for more coercions
     * and forward type information on the input expressions
     */
    private fun expectedInputsForExpectedOutput(
        expectedRet: Expectation,
        formalRet: Ty,
        formalArgs: List<Ty>,
    ): List<Ty> {
        @Suppress("NAME_SHADOWING")
        val formalRet = resolveTypeVarsWithObligations(formalRet)
        val retTy = expectedRet.onlyHasTy(ctx) ?: return emptyList()
        // Rustc does `fudge` instead of `probe` here. But `fudge` seems useless in our simplified type inference
        // because we don't produce new type variables during unification
        // https://github.com/rust-lang/rust/blob/50cf76c24bf6f266ca6d253a/compiler/rustc_infer/src/infer/fudge.rs#L98
        return ctx.probe {
            if (ctx.combineTypes(retTy, formalRet).isOk) {
                formalArgs.map { ctx.resolveTypeVarsIfPossible(it) }
            } else {
                emptyList()
            }
        }
    }

    // combineTypes with errors
    fun coerce(element: MvElement, inferred: Ty, expected: Ty): Boolean =
        coerceResolved(
            element,
            resolveTypeVarsWithObligations(inferred),
            resolveTypeVarsWithObligations(expected)
        )

    private fun coerceResolved(element: MvElement, inferred: Ty, expected: Ty): Boolean {
//        if (element is RsExpr) {
//            ctx.writeExpectedExprTyCoercable(element)
//        }
        return when (val result = ctx.tryCoerce(inferred, expected)) {
            is RsResult.Ok -> true
            is RsResult.Err -> when (val err = result.err) {
                is CombineTypeError.TypeMismatch -> {
                    checkTypeMismatch(err, element, inferred, expected)
                    false
                }
                is CombineTypeError.AbilitiesMismatch -> false
            }
        }
    }

//    private fun unknownTyFunction(arity: Int): TyFunction =
//        TyFunction(generateSequence { TyUnknown }.take(arity).toList(), TyUnknown)

    fun inferDotExprStructTy(dotExpr: MvDotExpr, parentCtx: InferenceContext): TyStruct? {
        val objectTy = inferExprTy(dotExpr.expr, parentCtx)
        return when (objectTy) {
            is TyReference -> objectTy.referenced as? TyStruct
            is TyStruct -> objectTy
            else -> null
        }
    }

    private fun inferDotExprTy(dotExpr: MvDotExpr, parentCtx: InferenceContext): Ty {
        val structTy = inferDotExprStructTy(dotExpr, parentCtx) ?: return TyUnknown
        val fieldName = dotExpr.structDotField.referenceName
        return structTy.fieldTy(fieldName)
    }

    fun inferStructLitExprTy(
        litExpr: MvStructLitExpr,
        parentCtx: InferenceContext,
        expected: Expectation
    ): Ty {
        val path = litExpr.path
        val structItem = path.maybeStruct ?: return TyUnknown
        val structTy = structItem.outerItemContext(parentCtx.msl).getStructItemTy(structItem)
            ?: return TyUnknown

        val inferenceCtx = InferenceContext(parentCtx.msl, parentCtx.itemContext)
        // find all types passed as explicit type parameters, create constraints with those
        if (path.typeArguments.isNotEmpty()) {
            if (path.typeArguments.size != structTy.typeVars.size) return TyUnknown
            for ((typeVar, typeArg) in structTy.typeVars.zip(path.typeArguments)) {
                val typeArgTy = parentCtx.getTypeTy(typeArg.type)

                // check compat for abilities
                val compat = isCompatibleAbilities(typeVar, typeArgTy, path.isMsl())
                val isCompat = when (compat) {
                    is Compat.AbilitiesMismatch -> {
                        parentCtx.typeErrors.add(
                            TypeError.AbilitiesMismatch(
                                typeArg,
                                typeArgTy,
                                compat.abilities
                            )
                        )
                        false
                    }

                    else -> true
                }
                inferenceCtx.registerEquateObligation(typeVar, if (isCompat) typeArgTy else TyUnknown)
            }
        }
        for (field in litExpr.fields) {
            val fieldName = field.referenceName
            val fieldTy = structTy.fieldTy(fieldName)
            inferLitFieldInitExprTy(field, parentCtx, fieldTy)
        }
        val expectedTy = expected.onlyHasTy(ctx)
        if (expectedTy != null) {
            inferenceCtx.registerEquateObligation(structTy, expectedTy)
        }
        inferenceCtx.processConstraints()

        parentCtx.resolveTyVarsFromContext(inferenceCtx)
        return inferenceCtx.resolveTy(structTy)
    }

    fun inferVectorLitExpr(litExpr: MvVectorLitExpr, parentCtx: InferenceContext): Ty {
        var tyVector = TyVector(TyInfer.TyVar())

        val inferenceCtx = InferenceContext(parentCtx.msl, parentCtx.itemContext)
        val typeArgument = litExpr.typeArgument

        if (typeArgument != null) {
            val ty = parentCtx.getTypeTy(typeArgument.type)
            inferenceCtx.registerEquateObligation(tyVector.item, ty)
        }
        val exprs = litExpr.vectorLitItems.exprList
        for (expr in exprs) {
            inferenceCtx.processConstraints()
            tyVector = inferenceCtx.resolveTy(tyVector) as TyVector

            val exprTy = inferExprTy(expr, parentCtx, Expectation.maybeHasType(tyVector.item))
            inferenceCtx.registerEquateObligation(tyVector.item, exprTy)
        }
        inferenceCtx.processConstraints()
        tyVector = inferenceCtx.resolveTy(tyVector) as TyVector
        return tyVector
    }

    fun inferLitFieldInitExprTy(
        litField: MvStructLitField,
        ctx: InferenceContext,
        expectedTy: Ty?
    ): Ty {
        val initExpr = litField.expr
        return if (initExpr == null) {
            // find type of binding
            val bindingPat =
                litField.reference.multiResolve().filterIsInstance<MvBindingPat>().firstOrNull()
                    ?: return TyUnknown
            val bindingPatTy = ctx.getPatType(bindingPat)
            if (expectedTy != null) {
                if (!isCompatible(expectedTy, bindingPatTy, ctx.msl)) {
                    ctx.typeErrors.add(TypeError.TypeMismatch(litField, expectedTy, bindingPatTy))
                } else {
                    ctx.registerEquateObligation(bindingPatTy, expectedTy)
                }
            }
            bindingPatTy
        } else {
            // find type of expression
            inferExprTy(initExpr, ctx, expectedTy)
        }
    }

    private fun inferBinaryExprTy(binaryExpr: MvBinaryExpr, ctx: InferenceContext): Ty {
        return when (binaryExpr.binaryOp.op) {
            "<", ">", "<=", ">=" -> inferOrderingBinaryExprTy(binaryExpr, ctx)
            "+", "-", "*", "/", "%" -> inferArithmeticBinaryExprTy(binaryExpr, ctx)
            "==", "!=" -> inferEqualityBinaryExprTy(binaryExpr, ctx)
            "||", "&&" -> inferLogicBinaryExprTy(binaryExpr, ctx)
            "==>", "<==>" -> TyBool
            else -> TyUnknown
        }
    }

    private fun inferArithmeticBinaryExprTy(binaryExpr: MvBinaryExpr, ctx: InferenceContext): Ty {
        val leftExpr = binaryExpr.left
        val rightExpr = binaryExpr.right
        val op = binaryExpr.binaryOp.op

        var typeErrorEncountered = false
        val leftExprTy = inferExprTy(leftExpr, ctx)
        if (!leftExprTy.supportsArithmeticOp()) {
            ctx.typeErrors.add(TypeError.UnsupportedBinaryOp(leftExpr, leftExprTy, op))
            typeErrorEncountered = true
        }
        if (rightExpr != null) {
            val rightExprTy = inferExprTy(rightExpr, ctx)
            if (!rightExprTy.supportsArithmeticOp()) {
                ctx.typeErrors.add(TypeError.UnsupportedBinaryOp(rightExpr, rightExprTy, op))
                typeErrorEncountered = true
            }

            if (leftExprTy is TyInteger && rightExprTy is TyInteger) {
                val compat = isCompatibleIntegers(leftExprTy, rightExprTy)
                if (!compat) {
                    ctx.typeErrors.add(
                        TypeError.IncompatibleArgumentsToBinaryExpr(
                            binaryExpr,
                            leftExprTy,
                            rightExprTy,
                            op
                        )
                    )
                    typeErrorEncountered = true
                }
            }

            if (!typeErrorEncountered) {
                ctx.registerEquateObligation(leftExprTy, rightExprTy)
            }
        }
        return if (typeErrorEncountered) TyUnknown else leftExprTy
    }

    private fun inferEqualityBinaryExprTy(binaryExpr: MvBinaryExpr, ctx: InferenceContext): Ty {
        val leftExpr = binaryExpr.left
        val rightExpr = binaryExpr.right
        val op = binaryExpr.binaryOp.op

        if (rightExpr != null) {
            val leftExprTy = inferExprTy(leftExpr, ctx)
            val rightExprTy = inferExprTy(rightExpr, ctx)
            if (!isCompatible(leftExprTy, rightExprTy)) {
                ctx.typeErrors.add(
                    TypeError.IncompatibleArgumentsToBinaryExpr(binaryExpr, leftExprTy, rightExprTy, op)
                )
            } else {
                ctx.registerEquateObligation(leftExprTy, rightExprTy)
            }
        }
        return TyBool
    }

    private fun inferOrderingBinaryExprTy(binaryExpr: MvBinaryExpr, ctx: InferenceContext): Ty {
        val leftExpr = binaryExpr.left
        val rightExpr = binaryExpr.right
        val op = binaryExpr.binaryOp.op

        var typeErrorEncountered = false
        val leftExprTy = inferExprTy(leftExpr, ctx)
        if (!leftExprTy.supportsOrdering()) {
            ctx.typeErrors.add(TypeError.UnsupportedBinaryOp(leftExpr, leftExprTy, op))
            typeErrorEncountered = true
        }
        if (rightExpr != null) {
            val rightExprTy = inferExprTy(rightExpr, ctx)
            if (!rightExprTy.supportsOrdering()) {
                ctx.typeErrors.add(TypeError.UnsupportedBinaryOp(rightExpr, rightExprTy, op))
                typeErrorEncountered = true
            }
            if (!typeErrorEncountered) {
                ctx.registerEquateObligation(leftExprTy, rightExprTy)
            }
        }

        return TyBool
    }

    private fun inferLogicBinaryExprTy(binaryExpr: MvBinaryExpr, ctx: InferenceContext): Ty {
        val leftExpr = binaryExpr.left
        val rightExpr = binaryExpr.right

        inferExprTy(leftExpr, ctx, Expectation.maybeHasType(TyBool))
        if (rightExpr != null) {
            inferExprTy(rightExpr, ctx, Expectation.maybeHasType(TyBool))
        }

        return TyBool
    }

    private fun Ty.supportsArithmeticOp(): Boolean {
        return this is TyInteger
                || this is TyNum
                || this is TyInfer.IntVar
                || this is TyUnknown
                || this is TyNever
    }

    private fun Ty.supportsOrdering(): Boolean {
        return this is TyInteger
                || this is TyNum
                || this is TyInfer.IntVar
                || this is TyUnknown
                || this is TyNever
    }

    private fun inferDerefExprTy(derefExpr: MvDerefExpr, ctx: InferenceContext): Ty {
        val exprTy =
            derefExpr.expr?.let { inferExprTy(it, ctx) }
        return (exprTy as? TyReference)?.referenced ?: TyUnknown
    }

    private fun inferTupleLitExprTy(tupleExpr: MvTupleLitExpr, ctx: InferenceContext): Ty {
        val types = tupleExpr.exprList.map { inferExprTy(it, ctx) }
        return TyTuple(types)
    }

    private fun inferLitExprTy(litExpr: MvLitExpr, ctx: InferenceContext): Ty {
        return when {
            litExpr.boolLiteral != null -> TyBool
            litExpr.addressLit != null -> TyAddress
            litExpr.integerLiteral != null || litExpr.hexIntegerLiteral != null -> {
                if (ctx.msl) return TyNum
                val literal = (litExpr.integerLiteral ?: litExpr.hexIntegerLiteral)!!
//            return TyInteger.fromSuffixedLiteral(literal) ?: TyInteger(TyInteger.DEFAULT_KIND)
                return TyInteger.fromSuffixedLiteral(literal) ?: TyInfer.IntVar()
            }

            litExpr.byteStringLiteral != null -> TyByteString(ctx.msl)
            litExpr.hexStringLiteral != null -> TyHexString(ctx.msl)
            else -> TyUnknown
        }
    }

    private fun inferIfExprTy(ifExpr: MvIfExpr, ctx: InferenceContext, expected: Expectation): Ty {
        val conditionExpr = ifExpr.condition?.expr
        if (conditionExpr != null) {
            inferExprTy(conditionExpr, ctx, Expectation.maybeHasType(TyBool))
        }

        val ifCodeBlock = ifExpr.codeBlock
        val ifInlineBlockExpr = ifExpr.inlineBlock?.expr
        val ifExprTy = when {
            ifCodeBlock != null -> {
                inferCodeBlockTy(ifCodeBlock, expected)
            }
            ifInlineBlockExpr != null -> {
                inferExprTy(ifInlineBlockExpr, ctx, expected)
            }
            else -> return TyUnknown
        }

        val elseBlock = ifExpr.elseBlock ?: return TyUnknown
        val elseCodeBlock = elseBlock.codeBlock
        val elseInlineBlockExpr = elseBlock.inlineBlock?.expr
        val elseExprTy = when {
            elseCodeBlock != null -> {
                val blockCtx = ctx.childContext()
                inferCodeBlockTy(elseCodeBlock, expected)
            }
            elseInlineBlockExpr != null -> {
                inferExprTy(elseInlineBlockExpr, ctx, expected)
            }
            else -> return TyUnknown
        }

        if (!isCompatible(ifExprTy, elseExprTy) && !isCompatible(elseExprTy, ifExprTy)) {
            val elseExpr = ifExpr.elseExpr
            if (elseExpr != null) {
                ctx.typeErrors.add(TypeError.TypeMismatch(elseExpr, ifExprTy, elseExprTy))
            }
            return TyUnknown
        }
        return combineTys(ifExprTy, elseExprTy, ctx.msl)
    }

    private fun inferWhileExpr(whileExpr: MvWhileExpr, ctx: InferenceContext): Ty {
        val conditionExpr = whileExpr.condition?.expr
        if (conditionExpr != null) {
            inferExprTy(conditionExpr, ctx, Expectation.maybeHasType(TyBool))
        }
        val codeBlock = whileExpr.codeBlock
        val inlineBlockExpr = whileExpr.inlineBlock?.expr
        when {
            codeBlock != null -> {
                val blockCtx = ctx.childContext()
                inferCodeBlockTy(codeBlock, blockCtx, TyUnit)
            }
            inlineBlockExpr != null -> inferExprTy(inlineBlockExpr, ctx, Expectation.maybeHasType(TyUnit))
        }
        return TyUnit
    }

    private fun inferLoopExpr(loopExpr: MvLoopExpr, ctx: InferenceContext): Ty {
        val codeBlock = loopExpr.codeBlock
        val inlineBlockExpr = loopExpr.inlineBlock?.expr
        when {
            codeBlock != null -> {
                val blockCtx = ctx.childContext()
                inferCodeBlockTy(codeBlock, blockCtx, TyUnit)
            }
            inlineBlockExpr != null -> inferExprTy(inlineBlockExpr, ctx, Expectation.maybeHasType(TyUnit))
        }
        return TyNever
    }

    private fun checkTypeMismatch(
        result: CombineTypeError.TypeMismatch,
        element: MvElement,
        inferred: Ty,
        expected: Ty
    ) {
        if (result.ty1.javaClass in IGNORED_TYS || result.ty2.javaClass in IGNORED_TYS) return
        if (expected is TyReference && inferred is TyReference &&
            (expected.containsTyOfClass(IGNORED_TYS) || inferred.containsTyOfClass(IGNORED_TYS))
        ) {
            // report errors with unknown types when &mut is needed, but & is present
            if (!(expected.permissions.contains(RefPermissions.WRITE)
                        && !inferred.permissions.contains(RefPermissions.WRITE))
            ) {
                return
            }
        }
        reportTypeMismatch(element, expected, inferred)
    }

    // Another awful hack: check that inner expressions did not annotated as an error
    // to disallow annotation intersections. This should be done in a different way
    private fun reportTypeMismatch(element: PsiElement, expected: Ty, inferred: Ty) {
        if (ctx.typeErrors.all { !element.isAncestorOf(it.element) }) {
            ctx.addTypeError(TypeError.TypeMismatch(element, expected, inferred))
        }
    }

    companion object {
        // ignoring possible false-positives (it's only basic experimental type checking)

        val IGNORED_TYS: List<Class<out Ty>> = listOf(
            TyUnknown::class.java,
            TyInfer.TyVar::class.java,
            TyTypeParameter::class.java,
        )
    }
}
