package org.move.ide.intentions

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.*
import org.move.lang.core.types.infer.InferenceContext
import org.move.lang.core.types.infer.inferCallExprTy
import org.move.lang.core.types.ty.TyFunction
import org.move.lang.core.types.ty.TyStruct

class AddAcquiresIntention : MvElementBaseIntentionAction<AddAcquiresIntention.Context>() {
    override fun getText(): String = "Add missing 'acquires' declaration"
    override fun getFamilyName(): String = text

    data class Context(
        val function: MvFunction,
        val importsOwner: MvImportStatementsOwner,
        val missingTy: TyStruct,
    )

    override fun findApplicableContext(
        project: Project,
        editor: Editor,
        element: PsiElement
    ): Context? {
        val callExpr = element.ancestorOrSelf<MvCallExpr>() ?: return null
        val funcTy = inferCallExprTy(callExpr, InferenceContext())
        if (funcTy !is TyFunction) return null
        if (funcTy.acquiresTypes.isEmpty()) return null

        val outFunction = callExpr.containingFunction ?: return null
        if (outFunction.script != null) return null

        val existingAcquiresTypes = outFunction.acquiresPathTypes.map { it.inferTypeTy() }
        val missingAcquiresTy = funcTy.acquiresTypes
            .find { it !in existingAcquiresTypes } as? TyStruct ?: return null

        val importsOwner = outFunction.containingImportsOwner ?: return null
        return Context(outFunction, importsOwner, missingAcquiresTy)
    }

    override fun invoke(project: Project, editor: Editor, ctx: Context) {
        val missingTyText = ctx.importsOwner.shortestPathIdentText(ctx.missingTy.item)
        val acquiresType = ctx.function.acquiresType
        if (acquiresType == null) {
            // can't be a native function
            val newAcquiresType = project.psiFactory.createAcquiresType("acquires $missingTyText")
            ctx.function.addBefore(newAcquiresType, ctx.function.codeBlock)
        } else {
            val acquiresTypeText = acquiresType.text.trimEnd(',')
            val newAcquiresType = MvPsiFactory(project)
                .createAcquiresType("$acquiresTypeText, $missingTyText")
            acquiresType.replace(newAcquiresType)
        }
    }
}
