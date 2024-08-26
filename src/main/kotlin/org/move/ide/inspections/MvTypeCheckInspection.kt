package org.move.ide.inspections

import com.intellij.codeInspection.ProblemHighlightType.GENERIC_ERROR
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.util.descendantsOfType
import com.intellij.psi.util.parents
import org.move.cli.settings.isTypeUnknownAsError
import org.move.lang.MvElementTypes.LAMBDA_EXPR
import org.move.lang.MvElementTypes.MODULE
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.*
import org.move.lang.core.resolve2.ref.InferenceCachedPathElement
import org.move.lang.core.types.infer.MvInferenceContextOwner
import org.move.lang.core.types.infer.TypeError
import org.move.lang.core.types.infer.inference
import org.move.lang.core.types.ty.TyUnknown

class MvTypeCheckInspection: MvLocalInspectionTool() {
    override fun buildMvVisitor(holder: ProblemsHolder, isOnTheFly: Boolean) =
        object: MvVisitor() {
            override fun visitItemSpec(o: MvItemSpec) = checkInferenceOwner(o, holder)
            override fun visitModuleItemSpec(o: MvModuleItemSpec) = checkInferenceOwner(o, holder)
            override fun visitFunction(o: MvFunction) = checkInferenceOwner(o, holder)
            override fun visitSpecFunction(o: MvSpecFunction) = checkInferenceOwner(o, holder)
            override fun visitSpecInlineFunction(o: MvSpecInlineFunction) = checkInferenceOwner(o, holder)
            override fun visitSchema(o: MvSchema) = checkInferenceOwner(o, holder)

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

            // debug only
            override fun visitExpr(o: MvExpr) {
                super.visitExpr(o)
                if (isTypeUnknownAsError()) {
                    checkForUnknownType(o, holder)
                }
            }
        }

    private fun checkInferenceOwner(inferenceOwner: MvInferenceContextOwner, holder: ProblemsHolder) {
        val msl = inferenceOwner.isMsl()
        val inference = inferenceOwner.inference(msl)
        var remainingErrors = inference.typeErrors
        inference.typeErrors
            .forEach { currentError ->
                val otherErrors = (remainingErrors - currentError)
                val element = currentError.element
                val skipError = otherErrors.any { filteredError ->
                    // todo: change to `withSelf = false` to deal with duplicate errors
                    val parents = filteredError.element.parents(withSelf = true)
                    // if any of the other errors contain deeper element, drop this one
                    // NOTE: if there's a duplicate, it remains in the tree (achieved with withSelf = false)
                    parents.contains(element)
                }
                // todo: drop this to deal with duplicate errors
                if (skipError) {
                    remainingErrors -= currentError
                }
                if (!skipError) {
                    holder.registerTypeError(currentError)
                }
            }
    }

    private fun ProblemsHolder.registerTypeError(typeError: TypeError) {
        this.registerProblem(
            typeError.element,
            typeError.message(),
            GENERIC_ERROR,
            *(listOfNotNull(typeError.fix()).toTypedArray())
        )
    }
}

private val UNIMPLEMENTED_TYPES = setOf(LAMBDA_EXPR)

private fun checkForUnknownType(o: MvExpr, holder: ProblemsHolder) {
    if (o.elementType in UNIMPLEMENTED_TYPES) return

    val msl = o.isMsl()
    val inference = o.inference(msl) ?: return

    // skip module references
    if (o is InferenceCachedPathElement) {
        val resolvedItems = inference.getResolvedPath(o.path).orEmpty()
        val resolvedElement = resolvedItems.singleOrNull()?.element
        if (resolvedElement?.elementType == MODULE) return
    }

    // skip pragmas
//    if (o.hasAncestor<MvPragmaSpecStmt>()) return

    // cannot type check correctly due to Intellij platform limitations
    if (o.isMsl()) return
//    if (o is MvPathExpr && (o.text == "result" || o.text.startsWith("result_"))) return

    // skip `_`
    if (o.text == "_") return

    val ty = inference.getExprType(o)
    if (ty is TyUnknown) {
        holder.registerProblem(o, "Element of unknown type (${o.elementType})", GENERIC_ERROR)
    }
}
