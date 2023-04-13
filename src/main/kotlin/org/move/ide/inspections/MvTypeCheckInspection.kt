package org.move.ide.inspections

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.codeInspection.util.InspectionMessage
import com.intellij.psi.PsiElement
import org.move.ide.presentation.name
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.isMsl
import org.move.lang.core.psi.ext.module
import org.move.lang.core.psi.ext.structItem
import org.move.lang.core.psi.ext.tyAbilities
import org.move.lang.core.types.infer.*
import org.move.lang.core.types.ty.TyStruct
import org.move.lang.core.types.ty.TyUnknown

class MvTypeCheckInspection : MvLocalInspectionTool() {
    override val isSyntaxOnly: Boolean get() = true

    override fun buildMvVisitor(holder: ProblemsHolder, isOnTheFly: Boolean) =
        object : MvVisitor() {
            override fun visitItemSpec(o: MvItemSpec) {
                val inference = o.maybeInferenceContext(true) ?: return
                inference.typeErrors
                    .filter { TypeError.isAllowedTypeError(it, TypeErrorScope.MAIN) }
                    .forEach {
                        holder.registerTypeError(it)
                    }
            }

            override fun visitFunction(o: MvFunction) {
                val msl = o.isMsl()
//                val inference = o.inference(msl)
                val inferenceCtx = o.inferenceContext(msl)
                inferenceCtx.typeErrors
//                inference.typeErrors
                    .filter { TypeError.isAllowedTypeError(it, TypeErrorScope.MAIN) }
                    .forEach {
                        holder.registerTypeError(it)
                    }
            }

            override fun visitModule(module: MvModule) {
                val itemContext = module.itemContext(false)
                itemContext.typeErrors
                    .filter { TypeError.isAllowedTypeError(it, TypeErrorScope.MODULE) }
                    .forEach {
                        holder.registerTypeError(it)
                    }
            }

            override fun visitStructField(field: MvStructField) {
                val structAbilities = field.structItem.tyAbilities
                if (structAbilities.isEmpty()) return

                val itemContext = field.structItem.module.itemContext(false)
                val fieldTy = itemContext.getStructFieldItemTy(field)
                    // explicit generic type of field has all abilities available
                    .foldTyInferWith { TyUnknown }

                val fieldAbilities = fieldTy.abilities()
                for (ability in structAbilities) {
                    val requiredAbility = ability.requires()
                    if (requiredAbility !in fieldAbilities) {
                        val message =
                            "The type '${fieldTy.name()}' does not have the ability '${requiredAbility.label()}' " +
                                    "required by the declared ability '${ability.label()}' " +
                                    "of the struct '${TyStruct(field.structItem, listOf(), mapOf(), listOf()).name()}'"
                        holder.registerTypeError(field, message)
                        return
                    }
                }
            }
        }

    fun ProblemsHolder.registerTypeError(
        element: PsiElement,
        @InspectionMessage message: String,
    ) {
        this.registerProblem(element, message, ProblemHighlightType.GENERIC_ERROR)
    }

    fun ProblemsHolder.registerTypeError(typeError: TypeError) {
        this.registerProblem(
            typeError.element,
            typeError.message(),
            ProblemHighlightType.GENERIC_ERROR,
        )
    }
}
