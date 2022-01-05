package org.move.ide.inspections

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import org.move.ide.annotator.ACQUIRES_BUILTIN_FUNCTIONS
import org.move.lang.core.psi.MvCallExpr
import org.move.lang.core.psi.MvVisitor
import org.move.lang.core.psi.containingFunction
import org.move.lang.core.psi.ext.fqName
import org.move.lang.core.psi.ext.inferTypeTy
import org.move.lang.core.psi.ext.typeArguments
import org.move.lang.core.psi.ext.typeFQNames
import org.move.lang.core.types.ty.TyStruct

class MvMissingAcquiresInspection: MvLocalInspectionTool() {
    override fun buildMvVisitor(holder: ProblemsHolder, isOnTheFly: Boolean) =
        object : MvVisitor() {
            override fun visitCallExpr(o: MvCallExpr) {
                if (o.path.referenceName !in ACQUIRES_BUILTIN_FUNCTIONS) return

                val paramType =
                    o.typeArguments.getOrNull(0)
                        ?.type?.inferTypeTy() as? TyStruct ?: return
                val paramTypeFQName = paramType.item.fqName
                val paramTypeName = paramType.item.name ?: return

                val containingFunction = o.containingFunction ?: return

                val name = containingFunction.name ?: return
                val errorMessage = "Function '$name' is not marked as 'acquires $paramTypeName'"
                val acquiresType = containingFunction.acquiresType
                if (acquiresType == null) {
                    holder.registerProblem(o, errorMessage, ProblemHighlightType.ERROR)
                    return
                }
                val acquiresTypeNames = acquiresType.typeFQNames ?: return
                if (paramTypeFQName !in acquiresTypeNames) {
                    holder.registerProblem(o, errorMessage, ProblemHighlightType.ERROR)
                }
            }
        }
}
