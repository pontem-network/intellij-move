package org.move.ide.intentions

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.move.ide.annotator.ACQUIRES_BUILTIN_FUNCTIONS
import org.move.ide.presentation.name
import org.move.lang.core.psi.MoveCallExpr
import org.move.lang.core.psi.MoveFunctionSignature
import org.move.lang.core.psi.MovePsiFactory
import org.move.lang.core.psi.ext.*
import org.move.lang.core.types.ty.TyStruct

class AddAcquiresIntention : MoveElementBaseIntentionAction<AddAcquiresIntention.Context>() {
    override fun getText(): String = "Add missing 'acquires' declaration"
    override fun getFamilyName(): String = text

    data class Context(
        val functionSignature: MoveFunctionSignature,
        val expectedAcquiresType: TyStruct
    )

    override fun findApplicableContext(
        project: Project,
        editor: Editor,
        element: PsiElement
    ): Context? {
        val callExpr = element.ancestorOrSelf<MoveCallExpr>() ?: return null
        if (callExpr.path.referenceName == null
            || callExpr.path.referenceName !in ACQUIRES_BUILTIN_FUNCTIONS
        ) return null
        if (callExpr.typeArguments.isEmpty()) return null
        val expectedAcquiresType =
            callExpr.typeArguments
                .getOrNull(0)?.type?.inferTypeTy() as? TyStruct ?: return null

        val outFunction = callExpr.containingFunction ?: return null
        val outFunctionSignature = outFunction.functionSignature ?: return null
        val acquiresType = outFunctionSignature.acquiresType

        val context = Context(outFunctionSignature, expectedAcquiresType)
        if (acquiresType == null) return context

        val acquiresTypeFQNames = acquiresType.typeFQNames ?: return null
        if (expectedAcquiresType.item.fqName in acquiresTypeFQNames) return null
        return context
    }

    override fun invoke(project: Project, editor: Editor, ctx: Context) {
        val acquiresType = ctx.functionSignature.acquiresType
        val expectedAcquiresTypeName = ctx.expectedAcquiresType.item.name!!
        if (acquiresType == null) {
            val newFunctionSignature =
                MovePsiFactory(project)
                    .createFunctionSignature("${ctx.functionSignature.text} " +
                                                     "acquires $expectedAcquiresTypeName")
            ctx.functionSignature.replace(newFunctionSignature)
        } else {
            val acquiresTypeText = acquiresType.text.trimEnd(',')
            val newAcquiresType = MovePsiFactory(project)
                .createAcquiresType("$acquiresTypeText, $expectedAcquiresTypeName")
            acquiresType.replace(newAcquiresType)
        }
    }
}
