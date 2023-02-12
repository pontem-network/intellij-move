package org.move.ide.inspections

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.codeInspection.util.InspectionMessage
import com.intellij.psi.PsiElement
import org.move.ide.presentation.name
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.*
import org.move.lang.core.types.infer.*
import org.move.lang.core.types.ty.TyStruct

class MvTypeCheckInspection : MvLocalInspectionTool() {
    override val isSyntaxOnly: Boolean get() = true

    override fun buildMvVisitor(holder: ProblemsHolder, isOnTheFly: Boolean) =
        object : MvVisitor() {
            override fun visitItemSpec(o: MvItemSpec) {
                val inference = o.ownerInferenceCtx(true) ?: return
                inference.typeErrors
                    .filter { TypeError.isAllowedTypeError(it, TypeErrorScope.MAIN) }
                    .forEach {
                        holder.registerTypeError(it)
                    }
            }

            override fun visitCodeBlock(codeBlock: MvCodeBlock) {
                val fn = codeBlock.parent as? MvFunction ?: return
                val inference = fn.ownerInferenceCtx(fn.isMsl()) ?: return
                inference.typeErrors
                    .filter { TypeError.isAllowedTypeError(it, TypeErrorScope.MAIN) }
                    .forEach {
                        holder.registerTypeError(it)
                    }
            }

            override fun visitStruct(s: MvStruct) {
                val itemContext = s.module.itemContext(false)
                itemContext.getItemTy(s)

                itemContext.typeErrors
                    .filter { TypeError.isAllowedTypeError(it, TypeErrorScope.MODULE) }
                    .forEach {
                        holder.registerTypeError(it)
                    }
            }

            override fun visitStructField(field: MvStructField) {
                val structAbilities = field.struct.tyAbilities
                if (structAbilities.isEmpty()) return

                val itemContext = field.struct.module.itemContext(false)
                val fieldTy = field.fieldAnnotationTy(itemContext) as? TyStruct ?: return

                for (ability in structAbilities) {
                    val requiredAbility = ability.requires()
                    if (requiredAbility !in fieldTy.abilities()) {
                        val message =
                            "The type '${fieldTy.name()}' does not have the ability '${requiredAbility.label()}' " +
                                    "required by the declared ability '${ability.label()}' " +
                                    "of the struct '${TyStruct(field.struct, listOf(), mapOf(), listOf()).name()}'"
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
