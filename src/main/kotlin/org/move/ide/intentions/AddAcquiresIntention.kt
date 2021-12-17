package org.move.ide.intentions

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.move.ide.annotator.ACQUIRES_BUILTIN_FUNCTIONS
import org.move.lang.MvElementTypes
import org.move.lang.core.psi.MvCallExpr
import org.move.lang.core.psi.MvFunction
import org.move.lang.core.psi.MvPsiFactory
import org.move.lang.core.psi.ext.*
import org.move.lang.core.psi.psiFactory
import org.move.lang.core.types.ty.TyStruct

class AddAcquiresIntention : MvElementBaseIntentionAction<AddAcquiresIntention.Context>() {
    override fun getText(): String = "Add missing 'acquires' declaration"
    override fun getFamilyName(): String = text

    data class Context(
        val function: MvFunction,
        val expectedAcquiresType: TyStruct
    )

    override fun findApplicableContext(
        project: Project,
        editor: Editor,
        element: PsiElement
    ): Context? {
        val callExpr = element.ancestorOrSelf<MvCallExpr>() ?: return null
        if (callExpr.path.referenceName == null
            || callExpr.path.referenceName !in ACQUIRES_BUILTIN_FUNCTIONS
        ) return null
        if (callExpr.typeArguments.isEmpty()) return null
        val expectedAcquiresType =
            callExpr.typeArguments
                .getOrNull(0)?.type?.inferTypeTy() as? TyStruct ?: return null

        val outFunction = callExpr.containingFunction ?: return null
        val acquiresType = outFunction.acquiresType

        val context = Context(outFunction, expectedAcquiresType)
        if (acquiresType == null) return context

        val acquiresTypeFQNames = acquiresType.typeFQNames ?: return null
        if (expectedAcquiresType.item.fqName in acquiresTypeFQNames) return null
        return context
    }

    override fun invoke(project: Project, editor: Editor, ctx: Context) {
        val acquiresType = ctx.function.acquiresType
        val expectedAcquiresTypeName = ctx.expectedAcquiresType.item.name!!
        if (acquiresType == null) {
            // is not a native function
            val newAcquiresType = project.psiFactory.createAcquiresType("acquires $expectedAcquiresTypeName")
            ctx.function.addBefore(newAcquiresType, ctx.function.codeBlock)
        } else {
            val acquiresTypeText = acquiresType.text.trimEnd(',')
            val newAcquiresType = MvPsiFactory(project)
                .createAcquiresType("$acquiresTypeText, $expectedAcquiresTypeName")
            acquiresType.replace(newAcquiresType)
        }
    }
}
