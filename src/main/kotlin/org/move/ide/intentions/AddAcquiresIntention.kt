package org.move.ide.intentions

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.move.lang.core.psi.MvCallExpr
import org.move.lang.core.psi.MvFunction
import org.move.lang.core.psi.MvPsiFactory
import org.move.lang.core.psi.ext.*
import org.move.lang.core.psi.psiFactory
import org.move.lang.core.types.infer.InferenceContext
import org.move.lang.core.types.infer.inferCallExprTy
import org.move.lang.core.types.ty.TyFunction
import org.move.lang.core.types.ty.TyStruct

class AddAcquiresIntention : MvElementBaseIntentionAction<AddAcquiresIntention.Context>() {
    override fun getText(): String = "Add missing 'acquires' declaration"
    override fun getFamilyName(): String = text

    data class Context(
        val function: MvFunction,
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

        return Context(outFunction, missingAcquiresTy)
    }

    override fun invoke(project: Project, editor: Editor, ctx: Context) {
        val acquiresType = ctx.function.acquiresType
        val missingTyName = ctx.missingTy.item.name!!
        if (acquiresType == null) {
            // can't be a native function
            val newAcquiresType = project.psiFactory.createAcquiresType("acquires $missingTyName")
            ctx.function.addBefore(newAcquiresType, ctx.function.codeBlock)
        } else {
            val acquiresTypeText = acquiresType.text.trimEnd(',')
            val newAcquiresType = MvPsiFactory(project)
                .createAcquiresType("$acquiresTypeText, $missingTyName")
            acquiresType.replace(newAcquiresType)
        }
    }
}
