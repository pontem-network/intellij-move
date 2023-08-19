package org.move.ide.inspections

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import org.move.ide.annotator.pluralise
import org.move.ide.presentation.name
import org.move.ide.presentation.text
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.abilities
import org.move.lang.core.psi.ext.fields
import org.move.lang.core.psi.ext.isMsl
import org.move.lang.core.psi.ext.structItem
import org.move.lang.core.types.infer.inferExpectedTypeArgumentTy
import org.move.lang.core.types.infer.inference
import org.move.lang.core.types.infer.loweredType
import org.move.lang.core.types.ty.GenericTy
import org.move.lang.core.types.ty.TyUnknown

class MvAbilityCheckInspection : MvLocalInspectionTool() {
    override fun buildMvVisitor(holder: ProblemsHolder, isOnTheFly: Boolean) =
        object : MvVisitor() {
            override fun visitValueArgumentList(o: MvValueArgumentList) {
                if (o.isMsl()) return

                val callExpr = o.parent as? MvCallExpr ?: return
                val funcItem = (callExpr.path.reference?.resolve() as? MvFunctionLike) ?: return
                val funcTy = funcItem.declaredType(false)
                val inference = callExpr.inference(false) ?: return

                for ((i, valueArgument) in o.valueArgumentList.withIndex()) {
                    val argumentExpr = valueArgument.expr ?: continue
                    val actualType = inference.getExprType(argumentExpr)
                    val expectedType = funcTy.paramTypes.getOrNull(i) ?: TyUnknown
                    val missingAbilities = expectedType.abilities() - actualType.abilities()
                    if (missingAbilities.isNotEmpty()) {
                        val abilitiesText =
                            missingAbilities.joinToString(prefix = "", postfix = "") { it.label() }
                        val message = "The type '${actualType.text()}' does not have required " +
                                "${pluralise(missingAbilities.size, "ability", "abilities")} " +
                                "'$abilitiesText'"
                        holder.registerProblem(argumentExpr, message, ProblemHighlightType.GENERIC_ERROR)
                    }
                }
            }

            override fun visitTypeArgumentList(o: MvTypeArgumentList) {
                if (o.isMsl()) return

                val path = o.parent as? MvPath ?: return
                val pathType = o.inference(false)?.getPathType(path) as? GenericTy ?: return
                val generics = pathType.item.generics

                for ((i, typeArgument) in o.typeArgumentList.withIndex()) {
                    val expectedType = inferExpectedTypeArgumentTy(typeArgument) ?: continue
                    val actualType = generics.getOrNull(i)?.let { pathType.substitution[it] } ?: continue

                    val missingAbilities = expectedType.abilities() - actualType.abilities()
                    if (missingAbilities.isNotEmpty()) {
                        val abilitiesText =
                            missingAbilities.joinToString(prefix = "", postfix = "") { it.label() }
                        val message = "The type '${actualType.text()}' does not have required " +
                                "${pluralise(missingAbilities.size, "ability", "abilities")} " +
                                "'$abilitiesText'"
                                holder.registerProblem(typeArgument, message, ProblemHighlightType.GENERIC_ERROR)
                    }
                }
            }

            override fun visitStruct(o: MvStruct) {
                val structAbilities = o.abilities
                if (structAbilities.isEmpty()) return
                for (field in o.fields) {
                    val fieldTy = field.type?.loweredType(false) ?: continue
                    val fieldAbilities = fieldTy.abilities()
                    for (ability in structAbilities) {
                        val requiredAbility = ability.requires()
                        if (requiredAbility !in fieldAbilities) {
                            val message =
                                "The type '${fieldTy.name()}' does not have the ability '${requiredAbility.label()}' " +
                                        "required by the declared ability '${ability.label()}' " +
                                        "of the struct '${field.structItem.name}'"
                            holder.registerProblem(field, message, ProblemHighlightType.GENERIC_ERROR)
                        }
                    }
                }
            }
        }
}
