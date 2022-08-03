package org.move.ide.intentions

import com.intellij.codeInspection.util.IntentionName
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.codeStyle.CodeStyleManager
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.startOffset

abstract class ChopListIntentionBase<TList : MvElement, TElement : MvElement>(
    listClass: Class<TList>,
    elementClass: Class<TElement>,
    @IntentionName intentionText: String
) : ListIntentionBase<TList, TElement>(listClass, elementClass, intentionText) {
    override fun findApplicableContext(project: Project, editor: Editor, element: PsiElement): TList? {
        val list = element.listContext ?: return null
        val elements = getElements(list)
        if (elements.size < 2 || elements.dropLast(1).all { hasLineBreakAfter(list, it) }) return null
        return list
    }

    override fun invoke(project: Project, editor: Editor, ctx: TList) {
        val document = editor.document
        val startOffset = ctx.startOffset

        val elements = getElements(ctx)
        val last = elements.last()
        if (!hasLineBreakAfter(ctx, last)) {
            getEndElement(ctx, last).textRange?.endOffset?.also { document.insertString(it, "\n") }
        }

        elements.asReversed().forEach {
            if (!hasLineBreakBefore(it)) {
                document.insertString(it.startOffset, "\n")
            }
        }
        val documentManager = PsiDocumentManager.getInstance(project)
        documentManager.commitDocument(document)
        val psiFile = documentManager.getPsiFile(document)
        if (psiFile != null) {
            psiFile.findElementAt(startOffset)?.listContext?.also {
                CodeStyleManager.getInstance(project).adjustLineIndent(psiFile, it.textRange)
            }
        }
    }
}

class ChopParameterListIntention : ChopListIntentionBase<MvFunctionParameterList, MvFunctionParameter>(
    MvFunctionParameterList::class.java,
    MvFunctionParameter::class.java,
    "Put parameters on separate lines"
)

class ChopArgumentListIntention : ChopListIntentionBase<MvCallArgumentList, MvExpr>(
    MvCallArgumentList::class.java,
    MvExpr::class.java,
    "Put arguments on separate lines"
)

class ChopAttrArgumentListIntention : ChopListIntentionBase<MvAttrItemArguments, MvAttrItemArgument>(
    MvAttrItemArguments::class.java,
    MvAttrItemArgument::class.java,
    "Put arguments on separate lines"
)

class ChopStructLiteralIntention : ChopListIntentionBase<MvStructLitFieldsBlock, MvStructLitField>(
    MvStructLitFieldsBlock::class.java,
    MvStructLitField::class.java,
    "Put fields on separate lines"
)
