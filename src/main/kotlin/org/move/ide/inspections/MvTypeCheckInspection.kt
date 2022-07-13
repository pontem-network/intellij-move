package org.move.ide.inspections

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.codeInspection.util.InspectionMessage
import com.intellij.psi.PsiElement
import org.move.ide.presentation.name
import org.move.ide.presentation.typeLabel
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.*
import org.move.lang.core.psi.mixins.ty
import org.move.lang.core.types.infer.inferCallExprTy
import org.move.lang.core.types.infer.inferenceCtx
import org.move.lang.core.types.infer.isCompatible
import org.move.lang.core.types.ty.*

fun ProblemsHolder.registerTypeError(
    element: PsiElement,
    @InspectionMessage message: String,
    vararg fixes: LocalQuickFix
) {
    this.registerProblem(element, message, ProblemHighlightType.GENERIC_ERROR, *fixes)
}

class MvTypeCheckInspection : MvLocalInspectionTool() {
    companion object {
        private fun invalidReturnTypeMessage(expectedTy: Ty, actualTy: Ty): String {
            return "Invalid return type: " +
                    "expected '${expectedTy.name()}', found '${actualTy.name()}'"
        }
    }

    override val isSyntaxOnly: Boolean get() = true

    override fun buildMvVisitor(holder: ProblemsHolder, isOnTheFly: Boolean) =
        object : MvVisitor() {
            override fun visitIfExpr(ifExpr: MvIfExpr) {
                val msl = ifExpr.isMsl()
                val ctx = ifExpr.inferenceCtx(msl)
                val ifTy = ifExpr.returningExpr?.inferExprTy(ctx) ?: return
                val elseExpr = ifExpr.elseExpr ?: return
                val elseTy = elseExpr.inferExprTy(ctx)

                if (!isCompatible(ifTy, elseTy) && !isCompatible(elseTy, ifTy)) {
                    holder.registerTypeError(
                        elseExpr, "Incompatible type '${elseTy.name()}'" +
                                ", expected '${ifTy.name()}'"
                    )
                }
            }

            override fun visitCondition(cond: MvCondition) {
                val msl = cond.isMsl()
                val expr = cond.expr ?: return
                val exprTy = expr.inferExprTy(cond.inferenceCtx(msl))
                if (!isCompatible(exprTy, TyBool)) {
                    holder.registerTypeError(
                        expr,
                        "Incompatible type '${exprTy.name()}', expected 'bool'"
                    )
                }
            }

            override fun visitCodeBlock(codeBlock: MvCodeBlock) {
                val msl = codeBlock.isMsl()
                val fn = codeBlock.parent as? MvFunction ?: return
                val returningExpr = codeBlock.returningExpr

                val expectedReturnTy = fn.returnTy
                val actualReturnTy = returningExpr?.inferExprTy(fn.inferenceCtx(msl)) ?: TyUnit
                if (!isCompatible(expectedReturnTy, actualReturnTy)) {
                    val annotatedElement = returningExpr as? PsiElement
                        ?: codeBlock.rightBrace
                        ?: codeBlock
                    holder.registerTypeError(
                        annotatedElement,
                        invalidReturnTypeMessage(expectedReturnTy, actualReturnTy)
                    )
                }
            }

            override fun visitReturnExpr(returnExpr: MvReturnExpr) {
                val msl = returnExpr.isMsl()
                val outerFn = returnExpr.containingFunction ?: return

                val ctx = outerFn.inferenceCtx(msl)
                val expectedReturnTy = outerFn.returnTy
                val actualReturnTy = returnExpr.expr?.inferExprTy(ctx) ?: return
                if (!isCompatible(expectedReturnTy, actualReturnTy)) {
                    holder.registerTypeError(
                        returnExpr,
                        invalidReturnTypeMessage(expectedReturnTy, actualReturnTy)
                    )
                }
            }

            override fun visitStructLitExpr(litExpr: MvStructLitExpr) {
                val struct = litExpr.path.maybeStruct ?: return

                val msl = litExpr.isMsl()
                val ctx = litExpr.inferenceCtx(msl)
                for (field in litExpr.fields) {
                    val initExprTy = field.inferInitExprTy(ctx)

                    val fieldName = field.referenceName
                    val fieldDef = struct.getField(fieldName) ?: continue
                    val expectedFieldTy = fieldDef.declaredTy(msl)

                    if (!isCompatible(expectedFieldTy, initExprTy)) {
                        val exprTypeName = initExprTy.name()
                        val expectedTypeName = expectedFieldTy.name()
                        val message =
                            "Invalid argument for field '$fieldName': " +
                                    "type '$exprTypeName' is not compatible with '$expectedTypeName'"
                        val initExpr = field.expr ?: field
                        holder.registerTypeError(initExpr, message)
                    }
                }
            }

            override fun visitPath(path: MvPath) {
                val typeArguments = path.typeArguments
                val item = path.reference?.resolve() as? MvTypeParametersOwner ?: return
                if (item.typeParameters.size != typeArguments.size) return

                for ((i, typeArgument) in typeArguments.withIndex()) {
                    val typeParam = item.typeParameters[i]
                    val argumentTy = typeArgument.type.ty()
                    checkHasRequiredAbilities(
                        holder,
                        typeArgument.type,
                        argumentTy,
                        typeParam.ty()
                    )
                }
            }

            override fun visitCallArgumentList(callArgs: MvCallArgumentList) {
                val callExpr = callArgs.parent as? MvCallExpr ?: return
                val function = callExpr.path.reference?.resolve() as? MvFunctionLike ?: return

                if (function.parameters.size != callArgs.exprList.size) return

                val msl = callArgs.isMsl()
                val ctx = callArgs.inferenceCtx(msl)
                val inferredFuncTy = inferCallExprTy(callExpr, ctx, null)
                if (inferredFuncTy !is TyFunction) return

                for ((i, expr) in callArgs.exprList.withIndex()) {
                    val parameter = function.parameters[i]
                    val paramTy = inferredFuncTy.paramTypes[i]
                    val exprTy = expr.inferExprTy(ctx)

                    if (paramTy.isTypeParam || exprTy.isTypeParam) {
                        val abilitiesErrorCreated = checkHasRequiredAbilities(holder, expr, exprTy, paramTy)
                        if (abilitiesErrorCreated) continue
                    }

                    if (!isCompatible(paramTy, exprTy)) {
                        val paramName = parameter.bindingPat.name ?: continue
                        val exprTypeName = exprTy.name()
                        val expectedTypeName = paramTy.name()
                        val message =
                            "Invalid argument for parameter '$paramName': " +
                                    "type '$exprTypeName' is not compatible with '$expectedTypeName'"
                        holder.registerTypeError(expr, message)
                    }
                }
            }

            override fun visitStructField(field: MvStructField) {
                val msl = field.isMsl()
                val structTy = TyStruct(field.struct)
                val structAbilities = structTy.abilities()
                if (structAbilities.isEmpty()) return

                val fieldTy = field.declaredTy(msl) as? TyStruct ?: return
                for (ability in structAbilities) {
                    val requiredAbility = ability.requires()
                    if (requiredAbility !in fieldTy.abilities()) {
                        val message =
                            "The type '${fieldTy.name()}' does not have the ability '${requiredAbility.label()}' " +
                                    "required by the declared ability '${ability.label()}' " +
                                    "of the struct '${structTy.name()}'"
                        holder.registerTypeError(field, message)
                        return
                    }
                }
            }
        }
}

private fun checkHasRequiredAbilities(
    holder: ProblemsHolder,
    element: MvElement,
    actualTy: Ty,
    expectedTy: Ty
): Boolean {
    // do not check for specs
    if (element.isMsl()) return false

    val abilities = actualTy.abilities()
    for (ability in expectedTy.abilities()) {
        if (ability !in abilities) {
            val typeName = actualTy.typeLabel(relativeTo = element)
            holder.registerTypeError(
                element,
                "The type '$typeName' " +
                        "does not have required ability '${ability.label()}'"
            )
            return true
        }
    }
    return false
}
