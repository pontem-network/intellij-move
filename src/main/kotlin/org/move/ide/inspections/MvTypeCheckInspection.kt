package org.move.ide.inspections

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemHighlightType.ERROR
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.util.descendantsOfType
import org.move.cli.settings.isDebugModeEnabled
import org.move.cli.settings.isTypeUnknownAsError
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.MvItemElement
import org.move.lang.core.psi.ext.isMsl
import org.move.lang.core.psi.ext.fieldOwner
import org.move.lang.core.psi.ext.itemElement
import org.move.lang.core.types.infer.TypeError
import org.move.lang.core.types.infer.inference
import org.move.lang.core.types.ty.TyUnknown
import java.rmi.registry.Registry

class MvTypeCheckInspection : MvLocalInspectionTool() {
    override fun buildMvVisitor(holder: ProblemsHolder, isOnTheFly: Boolean) =
        object : MvVisitor() {
            override fun visitExpr(o: MvExpr) {
                super.visitExpr(o)
                if (isTypeUnknownAsError()) {
                    val msl = o.isMsl()
                    val inference = o.inference(msl) ?: return
                    val ty = inference.getExprType(o)
                    if (ty is TyUnknown) {
                        holder.registerProblem(o, "Unknown type", ERROR)
                    }
                }
            }
            override fun visitItemSpec(o: MvItemSpec) {
                val inference = o.inference(true)
                inference.typeErrors
                    .forEach {
                        holder.registerTypeError(it)
                    }
            }

            override fun visitModuleItemSpec(o: MvModuleItemSpec) {
                val inference = o.inference(true)
                inference.typeErrors
                    .forEach {
                        holder.registerTypeError(it)
                    }
            }

            override fun visitFunction(o: MvFunction) {
                val inference = o.inference(o.isMsl())
                inference.typeErrors
                    .forEach {
                        holder.registerTypeError(it)
                    }
            }

            override fun visitSpecFunction(o: MvSpecFunction) {
                val inference = o.inference(true)
                inference.typeErrors
                    .forEach {
                        holder.registerTypeError(it)
                    }
            }

            override fun visitSpecInlineFunction(o: MvSpecInlineFunction) {
                val inference = o.inference(true)
                inference.typeErrors
                    .forEach {
                        holder.registerTypeError(it)
                    }
            }

            override fun visitSchema(o: MvSchema) {
                val inference = o.inference(true)
                inference.typeErrors
                    .forEach {
                        holder.registerTypeError(it)
                    }
            }

            override fun visitNamedFieldDecl(field: MvNamedFieldDecl) {
                val ownerItem = field.fieldOwner.itemElement as MvItemElement
                val fieldInnerTypes = field.type?.descendantsOfType<MvPathType>().orEmpty()
                for (fieldInnerType in fieldInnerTypes) {
                    val innerTypeItem =
                        fieldInnerType.path.reference?.resolve() as? MvItemElement ?: continue
                    if (innerTypeItem == ownerItem) {
                        holder.registerTypeError(TypeError.CircularType(fieldInnerType, ownerItem))
                    }
                }
            }
        }


    fun ProblemsHolder.registerTypeError(typeError: TypeError) {
        this.registerProblem(
            typeError.element,
            typeError.message(),
            ProblemHighlightType.GENERIC_ERROR,
            *(listOfNotNull(typeError.fix()).toTypedArray())
        )
    }
}
