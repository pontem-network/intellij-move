package org.move.lang.core.types.infer

import com.intellij.openapi.progress.ProgressManager
import com.intellij.psi.PsiElement
import org.move.cli.settings.pluginDevelopmentMode
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.*
import org.move.lang.core.types.ty.*
import org.move.stdext.RsResult
import org.move.stdext.chain

class TypeInferenceWalker(
    val ctx: InferenceContext,
    private val returnTy: Ty
) {

    private val fulfillmentContext: FulfillmentContext = ctx.fulfill

    val msl: Boolean = ctx.msl

    fun <T> mslScope(action: () -> T): T {
        if (ctx.msl) return action()
        ctx.msl = true
        val snapshot = ctx.startSnapshot()
        try {
            return action()
        } finally {
            ctx.msl = false
            snapshot.rollback()
        }
    }

    fun extractParameterBindings(owner: MvInferenceContextOwner) {
        val bindings = when (owner) {
            is MvFunctionLike -> owner.allParamsAsBindings
            is MvItemSpec -> {
                val funcItem = owner.funcItem
                funcItem?.allParamsAsBindings
                    ?.chain(funcItem.specResultParameters.map { it.bindingPat })
                    ?.toList().orEmpty()
            }
            is MvSchema -> owner.fieldStmts.map { it.bindingPat }
            else -> emptyList()
        }
        for (binding in bindings) {
            val bindingContext = binding.owner
            val ty = when (bindingContext) {
                is MvFunctionParameter -> bindingContext.type?.loweredType(msl) ?: TyUnknown
                is MvSchemaFieldStmt -> bindingContext.type?.loweredType(msl) ?: TyUnknown
                else -> TyUnknown
            }
            this.ctx.writePatTy(binding, ty)
        }
    }

    fun inferFnBody(block: AnyBlock): Ty = block.inferBlockCoercableTo(returnTy)
    fun inferSpec(block: AnyBlock): Ty =
        mslScope { block.inferBlockType(Expectation.NoExpectation) }

    private fun AnyBlock.inferBlockCoercableTo(expectedTy: Ty): Ty {
        return this.inferBlockCoercableTo(Expectation.maybeHasType(expectedTy))
    }

    private fun AnyBlock.inferBlockCoercableTo(expected: Expectation): Ty {
        return this.inferBlockType(expected, coerce = true)
    }

    private fun AnyBlock.inferBlockType(expected: Expectation, coerce: Boolean = false): Ty {
        val stmts = when (this) {
            is MvSpecCodeBlock, is MvModuleSpecBlock -> {
                // reorder stmts, move let stmts to the top, then let post, then others
                this.stmtList.sortedBy {
                    when {
                        it is MvLetStmt && !it.post -> 0
                        it is MvLetStmt && it.post -> 1
                        else -> 2
                    }
                }
            }
            else -> this.stmtList
        }
        stmts.forEach { processStatement(it) }

        val tailExpr = this.expr
        val expectedTy = expected.onlyHasTy(ctx)
        return if (tailExpr == null) {
            if (coerce && expectedTy != null) {
                coerce(this.rBrace ?: this, TyUnit, expectedTy)
            }
            TyUnit
        } else {
            if (coerce && expectedTy != null) {
                tailExpr.inferTypeCoerceTo(expectedTy)
            } else {
                tailExpr.inferType(expectedTy)
            }
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
                val explicitTy = stmt.type?.loweredType(msl)
                val expr = stmt.initializer?.expr
                val pat = stmt.pat
                val inferredTy =
                    if (expr != null) {
                        val inferredTy = inferExprTy(expr, Expectation.maybeHasType(explicitTy))
                        val coercedTy = if (explicitTy != null && coerce(expr, inferredTy, explicitTy)) {
                            explicitTy
                        } else {
                            inferredTy
                        }
                        coercedTy
                    } else {
                        pat?.anonymousTyVar() ?: TyUnknown
                    }
                pat?.extractBindings(
                    this,
                    explicitTy ?: resolveTypeVarsWithObligations(inferredTy)
                )
            }
            is MvSchemaFieldStmt -> {
                val binding = stmt.bindingPat
                val ty = stmt.type?.loweredType(msl) ?: TyUnknown
                ctx.writePatTy(binding, resolveTypeVarsWithObligations(ty))
            }
            is MvIncludeStmt -> inferIncludeStmt(stmt)
            is MvExprStmt -> stmt.expr.inferType()
            is MvSpecExprStmt -> stmt.expr.inferType()
        }
    }

    private fun MvExpr.inferType(expected: Ty?): Ty {
        return inferExprTy(this, Expectation.maybeHasType(expected))
    }

    private fun MvExpr.inferType(expected: Expectation = Expectation.NoExpectation): Ty {
        return inferExprTy(this, expected)
    }

    // returns inferred
    private fun MvExpr.inferTypeCoercableTo(expected: Ty): Ty {
        val inferred = this.inferType(expected)
        coerce(this, inferred, expected)
        return inferred
    }

    // returns expected
    private fun MvExpr.inferTypeCoerceTo(expected: Ty): Ty {
        val inferred = this.inferType(expected)
        return if (coerce(this, inferred, expected)) expected else inferred
    }

    private fun inferExprTy(
        expr: MvExpr,
        expected: Expectation = Expectation.NoExpectation
    ): Ty {
        ProgressManager.checkCanceled()
        if (ctx.isTypeInferred(expr)) error("Trying to infer expression type twice")

        expected.tyAsNullable(this.ctx)?.let {
            when (expr) {
                is MvStructLitExpr,
                is MvRefExpr,
                is MvDotExpr,
                is MvCallExpr -> this.ctx.writeExprExpectedTy(expr, it)
            }
        }

        val exprTy = when (expr) {
            is MvRefExpr -> inferRefExprTy(expr)
            is MvBorrowExpr -> inferBorrowExprTy(expr, expected)
            is MvCallExpr -> inferCallExprTy(expr, expected)
            is MvMacroCallExpr -> inferMacroCallExprTy(expr)
            is MvStructLitExpr -> inferStructLitExprTy(expr, expected)
            is MvVectorLitExpr -> inferVectorLitExpr(expr, expected)
            is MvIndexExpr -> inferIndexExprTy(expr)

            is MvDotExpr -> inferDotExprTy(expr)
            is MvDerefExpr -> inferDerefExprTy(expr)
            is MvLitExpr -> inferLitExprTy(expr)
            is MvTupleLitExpr -> inferTupleLitExprTy(expr, expected)
            is MvLambdaExpr -> inferLambdaExpr(expr, expected)

            is MvMoveExpr -> expr.expr?.inferType() ?: TyUnknown
            is MvCopyExpr -> expr.expr?.inferType() ?: TyUnknown

            is MvItemSpecBlockExpr -> expr.specBlock?.let { inferSpec(it) } ?: TyUnknown

            is MvCastExpr -> {
                expr.expr.inferType()
                val ty = expr.type.loweredType(msl)
                expected.onlyHasTy(this.ctx)?.let {
                    this.ctx.combineTypes(it, ty)
                }
                ty
            }
            is MvParensExpr -> expr.expr?.inferType(expected) ?: TyUnknown

            is MvBinaryExpr -> inferBinaryExprTy(expr)
            is MvBangExpr -> {
                expr.expr?.inferType(Expectation.maybeHasType(TyBool))
                TyBool
            }

            is MvIfExpr -> inferIfExprTy(expr, expected)
            is MvWhileExpr -> inferWhileExpr(expr)
            is MvLoopExpr -> inferLoopExpr(expr)
            is MvReturnExpr -> {
                expr.expr?.inferTypeCoercableTo(returnTy)
                TyNever
            }
            is MvContinueExpr -> TyNever
            is MvBreakExpr -> TyNever
            is MvAbortExpr -> {
                expr.expr?.inferTypeCoercableTo(TyInteger.DEFAULT)
                TyNever
            }
            is MvCodeBlockExpr -> expr.codeBlock.inferBlockType(expected)
            is MvAssignmentExpr -> inferAssignmentExprTy(expr)
            is MvBoolSpecExpr -> inferBoolSpecExpr(expr)
            is MvQuantExpr -> inferQuantExprTy(expr)
            is MvRangeExpr -> inferRangeExprTy(expr)
            is MvModifiesSpecExpr -> {
                expr.expr?.inferType()
                TyUnit
            }
            is MvAbortsWithSpecExpr -> {
                expr.exprList.forEach { it.inferTypeCoercableTo(TyInteger.DEFAULT) }
                TyUnit
            }
            is MvSpecVisRestrictedExpr -> expr.expr?.inferType(expected) ?: TyUnknown
            else ->
                if (expr.project.pluginDevelopmentMode) error(expr.typeErrorText) else TyUnknown
        }

        val refinedExprTy = exprTy.mslScopeRefined(msl)
        ctx.writeExprTy(expr, refinedExprTy)
        return refinedExprTy
    }

    private fun inferBoolSpecExpr(expr: MvBoolSpecExpr): Ty {
        expr.expr?.inferTypeCoercableTo(TyBool)
        if (expr is MvAbortsIfSpecExpr) {
            expr.abortsIfWith?.expr?.inferTypeCoercableTo(TyInteger.DEFAULT)
        }
        return TyUnit
    }

    private fun inferRefExprTy(refExpr: MvRefExpr): Ty {
        // special-case `result` inside item spec
        if (msl && refExpr.path.text == "result") {
            val funcItem = refExpr.ancestorStrict<MvItemSpec>()?.funcItem
            if (funcItem != null) {
                return funcItem.rawReturnType(true)
            }
        }
        val item = refExpr.path.reference?.resolveWithAliases()
        val ty = when (item) {
            is MvBindingPat -> ctx.getPatType(item)
            is MvConst -> item.type?.loweredType(msl) ?: TyUnknown
            else -> TyUnknown
        }
        return ty
    }

    private fun inferAssignmentExprTy(assignExpr: MvAssignmentExpr): Ty {
        val lhsTy = assignExpr.expr.inferType()
        assignExpr.initializer.expr?.inferTypeCoercableTo(lhsTy)
        return TyUnit
    }

    private fun inferBorrowExprTy(borrowExpr: MvBorrowExpr, expected: Expectation): Ty {
        val innerExpr = borrowExpr.expr ?: return TyUnknown
        val expectedInnerTy = (expected.onlyHasTy(ctx) as? TyReference)?.referenced
        val hint = Expectation.maybeHasType(expectedInnerTy)

        val innerExprTy = inferExprTy(innerExpr, hint)
        val mutabilities = RefPermissions.valueOf(borrowExpr.isMut)
        return TyReference(innerExprTy, mutabilities, ctx.msl)
    }

    private fun inferLambdaExpr(lambdaExpr: MvLambdaExpr, expected: Expectation): Ty {
        val bindings = lambdaExpr.bindingPatList
        val lambdaTy =
            (expected.onlyHasTy(this.ctx) as? TyLambda) ?: TyLambda.unknown(bindings.size)

        for ((i, binding) in lambdaExpr.bindingPatList.withIndex()) {
            val ty = lambdaTy.paramTypes.getOrElse(i) { TyUnknown }
            ctx.writePatTy(binding, ty)
        }
        lambdaExpr.expr?.inferTypeCoercableTo(lambdaTy.retType)
        return TyUnknown
    }

    private fun inferCallExprTy(callExpr: MvCallExpr, expected: Expectation): Ty {
        val path = callExpr.path
        val item = path.reference?.resolveWithAliases()
        val baseTy = when (item) {
            is MvFunctionLike -> {
                val (itemTy, _) = ctx.instantiatePath<TyFunction>(path, item)
                itemTy
            }
            is MvBindingPat -> {
                ctx.getPatType(item) as? TyLambda
                    ?: TyFunction.unknownTyFunction(callExpr.project, callExpr.valueArguments.size)
            }
            else -> TyFunction.unknownTyFunction(callExpr.project, callExpr.valueArguments.size)
        }
        val funcTy = ctx.resolveTypeVarsIfPossible(baseTy) as TyCallable

        val expectedInputTys =
            expectedInputsForExpectedOutput(expected, funcTy.retType, funcTy.paramTypes)

        inferArgumentTypes(funcTy.paramTypes, expectedInputTys, callExpr.callArgumentExprs)

        if (funcTy is TyFunction) {
            ctx.writeAcquiredTypes(callExpr, funcTy.acquiresTypes)
        }
        ctx.writeCallExprType(callExpr, funcTy as Ty)
        return funcTy.retType
    }

    private fun inferArgumentTypes(
        formalInputTys: List<Ty>,
        expectedInputTys: List<Ty>,
        argExprs: List<MvExpr>
    ) {
        for ((i, argExpr) in argExprs.withIndex()) {
            val formalInputTy = formalInputTys.getOrNull(i) ?: TyUnknown
            val expectedInputTy = expectedInputTys.getOrNull(i) ?: formalInputTy

            val expectation = Expectation.maybeHasType(expectedInputTy)
            val actualTy = inferExprTy(argExpr, expectation)
            val coercedTy =
                resolveTypeVarsWithObligations(expectation.onlyHasTy(ctx) ?: formalInputTy)
            coerce(argExpr, actualTy, coercedTy)

            // retrieve obligations
            ctx.combineTypes(formalInputTy, coercedTy)
        }
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
        val resolvedFormalRet = resolveTypeVarsWithObligations(formalRet)
        val retTy = expectedRet.onlyHasTy(ctx) ?: return emptyList()
        // Rustc does `fudge` instead of `probe` here. But `fudge` seems useless in our simplified type inference
        // because we don't produce new type variables during unification
        // https://github.com/rust-lang/rust/blob/50cf76c24bf6f266ca6d253a/compiler/rustc_infer/src/infer/fudge.rs#L98
        return ctx.probe {
            if (ctx.combineTypes(retTy, resolvedFormalRet).isOk) {
                formalArgs.map { ctx.resolveTypeVarsIfPossible(it) }
            } else {
                emptyList()
            }
        }
    }

    // combineTypes with errors
    fun coerce(element: PsiElement, inferred: Ty, expected: Ty): Boolean =
        coerceResolved(
            element,
            resolveTypeVarsWithObligations(inferred),
            resolveTypeVarsWithObligations(expected)
        )

    private fun coerceResolved(element: PsiElement, inferred: Ty, expected: Ty): Boolean {
        val coerceResult = ctx.tryCoerce(inferred, expected)
        return when (coerceResult) {
            is RsResult.Ok -> true
            is RsResult.Err -> when (val err = coerceResult.err) {
                is CombineTypeError.TypeMismatch -> {
                    checkTypeMismatch(err, element, inferred, expected)
                    false
                }
            }
        }
    }

    private fun inferDotExprTy(dotExpr: MvDotExpr): Ty {
        val baseTy = resolveTypeVarsWithObligations(dotExpr.expr.inferType())
        val structTy = when (baseTy) {
            is TyReference -> baseTy.innermostTy() as? TyStruct
            is TyStruct -> baseTy
            else -> null
        } ?: return TyUnknown

        val item = structTy.item
        val fieldName = dotExpr.structDotField.referenceName
        val fieldTy = item.fieldsMap[fieldName]
            ?.type
            ?.loweredType(msl)
            ?.substitute(structTy.typeParameterValues)
        return fieldTy ?: TyUnknown
    }

    fun inferStructLitExprTy(litExpr: MvStructLitExpr, expected: Expectation): Ty {
        val path = litExpr.path
        val structItem = path.maybeStruct
        if (structItem == null) {
            for (field in litExpr.fields) {
                field.expr?.let { inferExprTy(it) }
            }
            return TyUnknown
        }

        val (structTy, typeParameters) = ctx.instantiatePath<TyStruct>(path, structItem)
        expected.onlyHasTy(ctx)?.let { expectedTy ->
            ctx.unifySubst(typeParameters, expectedTy.typeParameterValues)
        }

        litExpr.fields.forEach { field ->
            val fieldTy = field.type(msl)?.substitute(structTy.substitution) ?: TyUnknown
            val expr = field.expr

            if (expr != null) {
                expr.inferTypeCoercableTo(fieldTy)
            } else {
                val bindingTy = field.resolveToBinding()?.let { ctx.getPatType(it) } ?: TyUnknown
                coerce(field, bindingTy, fieldTy)
            }
        }
        return structTy
    }

    private fun inferSchemaLitTy(schemaLit: MvSchemaLit): Ty {
        val path = schemaLit.path
        val schemaItem = path.maybeSchema
        if (schemaItem == null) {
            for (field in schemaLit.fields) {
                field.expr?.let { inferExprTy(it) }
            }
            return TyUnknown
        }

        val (schemaTy, _) = ctx.instantiatePath<TySchema>(path, schemaItem)
//        expected.onlyHasTy(ctx)?.let { expectedTy ->
//            ctx.unifySubst(typeParameters, expectedTy.typeParameterValues)
//        }

        schemaLit.fields.forEach { field ->
            val fieldTy = field.type(msl)?.substitute(schemaTy.substitution) ?: TyUnknown
            val expr = field.expr

            if (expr != null) {
                expr.inferTypeCoercableTo(fieldTy)
            } else {
                val bindingTy = field.resolveToBinding()?.let { ctx.getPatType(it) } ?: TyUnknown
                coerce(field, bindingTy, fieldTy)
            }
        }
        return schemaTy
    }

    private fun MvSchemaLitField.type(msl: Boolean) =
        this.resolveToDeclaration()?.type?.loweredType(msl)

    private fun MvStructLitField.type(msl: Boolean) =
        this.resolveToDeclaration()?.type?.loweredType(msl)

    fun inferVectorLitExpr(litExpr: MvVectorLitExpr, expected: Expectation): Ty {
        val tyVar = TyInfer.TyVar()
        val explicitTy = litExpr.typeArgument?.type?.loweredType(msl)
        if (explicitTy != null) {
            ctx.combineTypes(tyVar, explicitTy)
        }

        val exprs = litExpr.vectorLitItems.exprList
        val formalInputs = generateSequence { ctx.resolveTypeVarsIfPossible(tyVar) }.take(exprs.size).toList()
        val expectedInputTys =
            expectedInputsForExpectedOutput(expected, TyVector(tyVar), formalInputs)
        inferArgumentTypes(formalInputs, expectedInputTys, exprs)

        return ctx.resolveTypeVarsIfPossible(TyVector(tyVar))
    }

    private fun inferIndexExprTy(indexExpr: MvIndexExpr): Ty {
        val receiverExpr = indexExpr.exprList.first()
        val receiverTy = receiverExpr.inferTypeCoercableTo(TyVector(TyUnknown))

        val posExpr = indexExpr.exprList.drop(1).first()
        val posTy = posExpr.inferType()
        return when (posTy) {
            is TyIntegerRange -> (receiverTy as? TyVector) ?: TyUnknown
            is TyNum -> (receiverTy as? TyVector)?.item ?: TyUnknown
            else -> TyUnknown
        }
    }

    private fun inferQuantExprTy(quantExpr: MvQuantExpr): Ty {
        quantExpr.quantBindings?.quantBindingList.orEmpty()
            .forEach {
                collectQuantBinding(it)
            }
        quantExpr.expr?.inferTypeCoercableTo(TyBool)
        return TyBool
    }

    private fun collectQuantBinding(quantBinding: MvQuantBinding) {
        val bindingPat = quantBinding.bindingPat
        val ty = when (quantBinding) {
            is MvRangeQuantBinding -> {
                val rangeTy = quantBinding.expr?.inferType()
                when (rangeTy) {
                    is TyVector -> rangeTy.item
                    is TyIntegerRange -> TyInteger.DEFAULT
                    else -> TyUnknown
                }
            }
            is MvTypeQuantBinding -> quantBinding.type?.loweredType(true) ?: TyUnknown
            else -> error("unreachable")
        }
        this.ctx.writePatTy(bindingPat, ty)
    }

    private fun inferRangeExprTy(rangeExpr: MvRangeExpr): Ty {
        rangeExpr.exprList.first().inferTypeCoercableTo(TyInteger.DEFAULT)
        rangeExpr.exprList.drop(1).first().inferTypeCoercableTo(TyInteger.DEFAULT)
        return TyIntegerRange
    }

    private fun inferBinaryExprTy(binaryExpr: MvBinaryExpr): Ty {
        return when (binaryExpr.binaryOp.op) {
            "<", ">", "<=", ">=" -> inferOrderingBinaryExprTy(binaryExpr)
            "+", "-", "*", "/", "%" -> inferArithmeticBinaryExprTy(binaryExpr)
            "==", "!=" -> inferEqualityBinaryExprTy(binaryExpr)
            "||", "&&", "==>", "<==>" -> inferLogicBinaryExprTy(binaryExpr)
            "^", "|", "&" -> inferBitOpsExprTy(binaryExpr)
            "<<", ">>" -> inferBitShiftsExprTy(binaryExpr)
            else -> TyUnknown
        }
    }

    private fun inferArithmeticBinaryExprTy(binaryExpr: MvBinaryExpr): Ty {
        val leftExpr = binaryExpr.left
        val rightExpr = binaryExpr.right
        val op = binaryExpr.binaryOp.op

        var typeErrorEncountered = false
        val leftTy = leftExpr.inferType()
        if (!leftTy.supportsArithmeticOp()) {
            ctx.reportTypeError(TypeError.UnsupportedBinaryOp(leftExpr, leftTy, op))
            typeErrorEncountered = true
        }
        if (rightExpr != null) {
            val rightTy = rightExpr.inferType()
            if (!rightTy.supportsArithmeticOp()) {
                ctx.reportTypeError(TypeError.UnsupportedBinaryOp(rightExpr, rightTy, op))
                typeErrorEncountered = true
            }

            if (!typeErrorEncountered && ctx.combineTypes(leftTy, rightTy).isErr) {
                ctx.reportTypeError(
                    TypeError.IncompatibleArgumentsToBinaryExpr(
                        binaryExpr,
                        leftTy,
                        rightTy,
                        op
                    )
                )
                typeErrorEncountered = true
            }
        }
        return if (typeErrorEncountered) TyUnknown else leftTy
    }

    private fun inferEqualityBinaryExprTy(binaryExpr: MvBinaryExpr): Ty {
        val leftExpr = binaryExpr.left
        val rightExpr = binaryExpr.right
        val op = binaryExpr.binaryOp.op

        if (rightExpr != null) {
            val leftTy = inferExprTy(leftExpr)
            val rightTy = inferExprTy(rightExpr)
            if (ctx.combineTypes(leftTy, rightTy).isErr) {
                ctx.reportTypeError(
                    TypeError.IncompatibleArgumentsToBinaryExpr(binaryExpr, leftTy, rightTy, op)
                )
            }
        }
        return TyBool
    }

    private fun inferOrderingBinaryExprTy(binaryExpr: MvBinaryExpr): Ty {
        val leftExpr = binaryExpr.left
        val rightExpr = binaryExpr.right
        val op = binaryExpr.binaryOp.op

        var typeErrorEncountered = false
        val leftTy = inferExprTy(leftExpr)
        if (!leftTy.supportsOrdering()) {
            ctx.reportTypeError(TypeError.UnsupportedBinaryOp(leftExpr, leftTy, op))
            typeErrorEncountered = true
        }
        if (rightExpr != null) {
            val rightTy = inferExprTy(rightExpr)
            if (!rightTy.supportsOrdering()) {
                ctx.reportTypeError(TypeError.UnsupportedBinaryOp(rightExpr, rightTy, op))
                typeErrorEncountered = true
            }
            if (!typeErrorEncountered) {
                coerce(rightExpr, rightTy, expected = leftTy)
            }
        }
        return TyBool
    }

    private fun inferLogicBinaryExprTy(binaryExpr: MvBinaryExpr): Ty {
        val leftExpr = binaryExpr.left
        val rightExpr = binaryExpr.right

        leftExpr.inferTypeCoercableTo(TyBool)
        if (rightExpr != null) {
            rightExpr.inferTypeCoercableTo(TyBool)
        }
        return TyBool
    }

    private fun inferBitOpsExprTy(binaryExpr: MvBinaryExpr): Ty {
        val leftExpr = binaryExpr.left
        val rightExpr = binaryExpr.right

        val leftTy = leftExpr.inferTypeCoercableTo(TyInteger.DEFAULT)
        if (rightExpr != null) {
            rightExpr.inferTypeCoercableTo(leftTy)
        }
        return leftTy
    }

    private fun inferBitShiftsExprTy(binaryExpr: MvBinaryExpr): Ty {
        val leftExpr = binaryExpr.left
        val rightExpr = binaryExpr.right

        val leftTy = leftExpr.inferTypeCoercableTo(TyInteger.DEFAULT)
        if (rightExpr != null) {
            rightExpr.inferTypeCoercableTo(TyInteger.U8)
        }
        return leftTy
    }

    private fun Ty.supportsArithmeticOp(): Boolean {
        val ty = resolveTypeVarsWithObligations(this)
        return ty is TyInteger
                || ty is TyNum
                || ty is TyInfer.IntVar
                || ty is TyUnknown
                || ty is TyNever
    }

    private fun Ty.supportsOrdering(): Boolean {
        val ty = resolveTypeVarsWithObligations(this)
        return ty is TyInteger
                || ty is TyNum
                || ty is TyInfer.IntVar
                || ty is TyUnknown
                || ty is TyNever
    }

    private fun inferDerefExprTy(derefExpr: MvDerefExpr): Ty {
        val exprTy = derefExpr.expr?.inferType()
        return (exprTy as? TyReference)?.referenced ?: TyUnknown
    }

    private fun inferTupleLitExprTy(tupleExpr: MvTupleLitExpr, expected: Expectation): Ty {
        val types = tupleExpr.exprList.mapIndexed { i, itemExpr ->
            val expectedTy = (expected.onlyHasTy(ctx) as? TyTuple)?.types?.getOrNull(i)
            if (expectedTy != null) {
                itemExpr.inferTypeCoerceTo(expectedTy)
            } else {
                itemExpr.inferType()
            }
        }
        return TyTuple(types)
    }

    private fun inferLitExprTy(litExpr: MvLitExpr): Ty {
        return when {
            litExpr.boolLiteral != null -> TyBool
            litExpr.addressLit != null -> TyAddress
            litExpr.integerLiteral != null || litExpr.hexIntegerLiteral != null -> {
                if (ctx.msl) return TyNum
                val literal = (litExpr.integerLiteral ?: litExpr.hexIntegerLiteral)!!
                return TyInteger.fromSuffixedLiteral(literal) ?: TyInfer.IntVar()
            }
            litExpr.byteStringLiteral != null -> TyByteString(ctx.msl)
            litExpr.hexStringLiteral != null -> TyHexString(ctx.msl)
            else -> TyUnknown
        }
    }

    private fun inferIfExprTy(ifExpr: MvIfExpr, expected: Expectation): Ty {
        ifExpr.condition?.expr?.inferTypeCoercableTo(TyBool)

        val ifTy =
            ifExpr.codeBlock?.inferBlockType(expected)
                ?: ifExpr.inlineBlock?.expr?.inferType(expected)

        val elseBlock = ifExpr.elseBlock ?: return TyUnit

        val resolvedIfTy = resolveTypeVarsWithObligations(ifTy ?: TyUnknown)
        val expectedElse = Expectation.maybeHasType(resolvedIfTy)
        val elseTy =
            elseBlock.codeBlock?.inferBlockType(expected)
                ?: elseBlock.inlineBlock?.expr?.inferType(expectedElse)
        if (ifTy != null && elseTy != null) {
            val element = elseBlock.codeBlock ?: elseBlock.inlineBlock!!
            // special case for references in if-else block
            if (ifTy is TyReference && elseTy is TyReference) {
                coerce(element, elseTy.referenced, ifTy.referenced)
            } else {
                coerce(element, elseTy, ifTy)
            }
        }
        return intersectTypes(listOfNotNull(ifTy, elseTy), symmetric = true)
    }

    private fun intersectTypes(types: List<Ty>, symmetric: Boolean = true): Ty {
        if (types.isEmpty()) return TyUnknown
        return types.reduce { acc, ty -> intersectTypes(acc, ty, symmetric) }
    }

    private fun intersectTypes(ty1: Ty, ty2: Ty, symmetric: Boolean = true): Ty {
        return when {
            ty1 is TyNever -> ty2
            ty2 is TyNever -> ty1
            ty1 is TyUnknown -> if (ty2 !is TyNever) ty2 else TyUnknown
            else -> {
                val ok = ctx.combineTypes(ty1, ty2).isOk
                        || if (symmetric) ctx.combineTypes(ty2, ty1).isOk else false
                if (ok) {
                    when {
                        ty1 is TyReference && ty2 is TyReference -> {
                            val combined = ty1.permissions.intersect(ty2.permissions)
                            TyReference(ty1.referenced, combined, ty1.msl || ty2.msl)
                        }
                        else -> ty1
                    }
                } else {
                    TyUnknown
                }
            }
        }
    }

    private fun inferWhileExpr(whileExpr: MvWhileExpr): Ty {
        val conditionExpr = whileExpr.condition?.expr
        if (conditionExpr != null) {
            conditionExpr.inferTypeCoercableTo(TyBool)
        }
        val codeBlock = whileExpr.codeBlock
        val inlineBlockExpr = whileExpr.inlineBlock?.expr

        val expected = Expectation.maybeHasType(TyUnit)
        when {
            codeBlock != null -> codeBlock.inferBlockType(expected)
            inlineBlockExpr != null -> inlineBlockExpr.inferType(expected)
        }
        return TyUnit
    }

    private fun inferLoopExpr(loopExpr: MvLoopExpr): Ty {
        val codeBlock = loopExpr.codeBlock
        val inlineBlockExpr = loopExpr.inlineBlock?.expr
        val expected = Expectation.maybeHasType(TyUnit)
        when {
            codeBlock != null -> codeBlock.inferBlockType(expected)
            inlineBlockExpr != null -> inlineBlockExpr.inferType(expected)
        }
        return TyNever
    }

    private fun inferIncludeStmt(includeStmt: MvIncludeStmt) {
        val includeItem = includeStmt.includeItem ?: return
        when (includeItem) {
            is MvSchemaIncludeItem -> inferSchemaLitTy(includeItem.schemaLit)
            is MvAndIncludeItem -> {
                includeItem.schemaLitList.forEach { inferSchemaLitTy(it) }
            }
            is MvIfElseIncludeItem -> {
                includeItem.condition.expr?.inferTypeCoercableTo(TyBool)
                includeItem.schemaLitList.forEach { inferSchemaLitTy(it) }
            }
            is MvImplyIncludeItem -> {
                includeItem.childOfType<MvExpr>()?.inferTypeCoercableTo(TyBool)
                inferSchemaLitTy(includeItem.schemaLit)
            }
            else -> error("unreachable")
        }
    }

    private fun checkTypeMismatch(
        result: CombineTypeError.TypeMismatch,
        element: PsiElement,
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
        reportTypeError(TypeError.TypeMismatch(element, expected, inferred))
//        if (ctx.typeErrors.all { !element.isAncestorOf(it.element) }) {
//            ctx.addTypeError(TypeError.TypeMismatch(element, expected, inferred))
//        }
    }

    // Another awful hack: check that inner expressions did not annotated as an error
    // to disallow annotation intersections. This should be done in a different way
    fun reportTypeError(typeError: TypeError) = ctx.reportTypeError(typeError)

    companion object {
        // ignoring possible false-positives (it's only basic experimental type checking)

        val IGNORED_TYS: List<Class<out Ty>> = listOf(
            TyUnknown::class.java,
            TyInfer.TyVar::class.java,
//            TyTypeParameter::class.java,
        )
    }
}
