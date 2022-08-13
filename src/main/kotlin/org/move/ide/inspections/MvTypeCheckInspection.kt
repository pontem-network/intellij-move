package org.move.ide.inspections

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.codeInspection.util.InspectionMessage
import com.intellij.psi.PsiElement
import org.move.ide.presentation.name
import org.move.ide.presentation.typeLabel
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.*
import org.move.lang.core.psi.mixins.ty
import org.move.lang.core.types.infer.*
import org.move.lang.core.types.ty.*

fun ProblemsHolder.registerTypeError(
    element: PsiElement,
    @InspectionMessage message: String,
) {
    this.registerProblem(element, message, ProblemHighlightType.GENERIC_ERROR)
}

fun ProblemsHolder.registerTypeError(typeError: TypeError) {
    this.registerProblem(typeError.element, typeError.message(), ProblemHighlightType.GENERIC_ERROR)
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
                val ifTy = ifExpr.returningExpr?.inferredTy() ?: return
                val elseExpr = ifExpr.elseExpr ?: return
                val elseTy = elseExpr.inferredTy()

                if (!isCompatible(ifTy, elseTy) && !isCompatible(elseTy, ifTy)) {
                    holder.registerTypeError(
                        elseExpr, "Incompatible type '${elseTy.name()}'" +
                                ", expected '${ifTy.name()}'"
                    )
                }
            }

            override fun visitCondition(cond: MvCondition) {
                val expr = cond.expr ?: return
                val exprTy = expr.inferredTy()
                if (!isCompatible(exprTy, TyBool)) {
                    holder.registerTypeError(
                        expr,
                        "Incompatible type '${exprTy.name()}', expected 'bool'"
                    )
                }
            }

            override fun visitCodeBlock(codeBlock: MvCodeBlock) {
                val fn = codeBlock.parent as? MvFunction ?: return
                val returningExpr = codeBlock.returningExpr

                val expectedReturnTy = fn.returnTy
                val actualReturnTy = returningExpr?.inferredTy() ?: TyUnit
                if (!isCompatible(expectedReturnTy, actualReturnTy)) {
                    val annotatedElement = returningExpr as? PsiElement
                        ?: codeBlock.rightBrace
                        ?: codeBlock
                    holder.registerTypeError(
                        annotatedElement,
                        invalidReturnTypeMessage(expectedReturnTy, actualReturnTy)
                    )
                }

                val inference = fn.inferenceCtx(codeBlock.isMsl())
                inference.typeErrors
                    .forEach { holder.registerTypeError(it) }
            }

            override fun visitReturnExpr(returnExpr: MvReturnExpr) {
                val outerFn = returnExpr.containingFunction ?: return

                val expectedReturnTy = outerFn.returnTy
                val actualReturnTy = returnExpr.expr?.inferredTy() ?: return
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
                val ctx = litExpr.functionInferenceCtx(msl)
                for (field in litExpr.fields) {
                    val initExprTy = inferLitFieldInitExprTy(field, ctx)

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

            override fun visitCallArgumentList(callArgs: MvCallArgumentList) {
                val callExpr = callArgs.parent as? MvCallExpr ?: return
                val function = callExpr.path.reference?.resolve() as? MvFunctionLike ?: return

                if (function.parameters.size != callArgs.exprList.size) return

                val msl = callArgs.isMsl()
                val ctx = callArgs.functionInferenceCtx(msl)
                val inferredFuncTy = inferCallExprTy(callExpr, ctx, null) as? TyFunction ?: return

                for ((i, expr) in callArgs.exprList.withIndex()) {
                    val parameter = function.parameters[i]
                    val paramTy = inferredFuncTy.paramTypes[i]
                    val exprTy = expr.inferredTy()

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
                val structTy = instantiateItemTy(field.struct, msl)
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
