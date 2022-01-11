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
import org.move.lang.core.types.infer.InferenceContext
import org.move.lang.core.types.infer.combineTys
import org.move.lang.core.types.infer.inferCallExprTy
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

    override fun buildMvVisitor(holder: ProblemsHolder, isOnTheFly: Boolean) =
        object : MvVisitor() {
            override fun visitIfExpr(o: MvIfExpr) {
                val ifTy = o.returningExpr?.inferExprTy() ?: return
                val elseExpr = o.elseExpr ?: return
                val elseTy = elseExpr.inferExprTy()

                if (!isCompatible(ifTy, elseTy) && !isCompatible(elseTy, ifTy)) {
                    holder.registerTypeError(
                        elseExpr, "Incompatible type '${elseTy.typeLabel(o)}'" +
                                ", expected '${ifTy.typeLabel(o)}'"
                    )
                }
            }

            override fun visitCondition(o: MvCondition) {
                val expr = o.expr ?: return
                val exprTy = expr.inferExprTy()
                if (!isCompatible(exprTy, TyBool)) {
                    holder.registerTypeError(
                        expr,
                        "Incompatible type '${exprTy.typeLabel(o)}', expected 'bool'"
                    )
                }
            }

            override fun visitCodeBlock(codeBlock: MvCodeBlock) {
                val fn = codeBlock.parent as? MvFunction ?: return
                val returningExpr = codeBlock.returningExpr

                val expectedReturnTy = fn.resolvedReturnTy
                val actualReturnTy = returningExpr?.inferExprTy() ?: TyUnit
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

            override fun visitReturnExpr(o: MvReturnExpr) {
                val outerFn = o.containingFunction ?: return
                val expectedReturnTy = outerFn.resolvedReturnTy
                val actualReturnTy = o.expr?.inferExprTy() ?: return
                if (!isCompatible(expectedReturnTy, actualReturnTy)) {
                    holder.registerTypeError(
                        o,
                        invalidReturnTypeMessage(expectedReturnTy, actualReturnTy)
                    )
                }
            }

            override fun visitStructLitExpr(o: MvStructLitExpr) {
                val struct = o.path.maybeStruct ?: return

                val ctx = InferenceContext()
                for (field in o.fields) {
                    val initExprTy = field.inferInitExprTy(ctx)

                    val fieldName = field.referenceName
                    val fieldDef = struct.getField(fieldName) ?: continue
                    val expectedFieldTy = fieldDef.declaredTy

                    if (!isCompatible(expectedFieldTy, initExprTy)) {
                        val exprTypeName = initExprTy.typeLabel(relativeTo = o)
                        val expectedTypeName = expectedFieldTy.typeLabel(relativeTo = o)

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

            override fun visitCallArgumentList(arguments: MvCallArgumentList) {
                val callExpr = arguments.parent as? MvCallExpr ?: return
                val function = callExpr.path.reference?.resolve() as? MvFunction ?: return

                if (function.parameters.size != arguments.exprList.size) return

                val ctx = InferenceContext()
                val inferredFuncTy = inferCallExprTy(callExpr, ctx)
                if (inferredFuncTy !is TyFunction) return

                for ((i, expr) in arguments.exprList.withIndex()) {
                    val parameter = function.parameters[i]
                    val paramTy = inferredFuncTy.paramTypes[i]
                    val exprTy = expr.inferExprTy(ctx)

                    if (paramTy.isTypeParam || exprTy.isTypeParam) {
                        val abilitiesErrorCreated = checkHasRequiredAbilities(holder, expr, exprTy, paramTy)
                        if (abilitiesErrorCreated) continue
                    }

                    if (!isCompatible(paramTy, exprTy)) {
                        val paramName = parameter.bindingPat.name ?: continue
                        val exprTypeName = exprTy.typeLabel(relativeTo = arguments)
                        val expectedTypeName = paramTy.typeLabel(relativeTo = arguments)

                        val message =
                            "Invalid argument for parameter '$paramName': " +
                                    "type '$exprTypeName' is not compatible with '$expectedTypeName'"
                        holder.registerTypeError(expr, message)
                    }
                }
            }

            override fun visitStructFieldDef(structField: MvStructFieldDef) {
                val structTy = TyStruct(structField.struct)
                val structAbilities = structTy.abilities()
                if (structAbilities.isEmpty()) return

                val fieldTy = structField.declaredTy as? TyStruct ?: return
                for (ability in structAbilities) {
                    val requiredAbility = ability.requires()
                    if (requiredAbility !in fieldTy.abilities()) {
                        val message =
                            "The type '${fieldTy.name()}' does not have the ability '${requiredAbility.label()}' " +
                                    "required by the declared ability '${ability.label()}' " +
                                    "of the struct '${structTy.name()}'"
                        holder.registerTypeError(structField, message)
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
    if (element.isInsideSpecBlock()) return false

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
