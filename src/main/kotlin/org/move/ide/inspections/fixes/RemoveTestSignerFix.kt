package org.move.ide.inspections.fixes

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import org.move.ide.inspections.DiagnosticFix
import org.move.lang.MvElementTypes
import org.move.lang.core.psi.MvAttrItemArgument
import org.move.lang.core.psi.MvAttrItemArguments
import org.move.lang.core.psi.ext.elementType
import org.move.lang.core.psi.ext.getNextNonCommentSibling
import org.move.lang.core.psi.ext.getPrevNonCommentSibling

class RemoveTestSignerFix(
    itemArgument: MvAttrItemArgument,
    val signerName: String
) : DiagnosticFix<MvAttrItemArgument>(itemArgument) {

    override fun getText(): String = "Remove '$signerName'"
    override fun getFamilyName(): String = "Remove unused test signer"

    override fun invoke(project: Project, file: PsiFile, element: MvAttrItemArgument) {
        val argument = element
        val container = element.parent as MvAttrItemArguments

        // remove trailing comma
        argument.getNextNonCommentSibling()
            ?.takeIf { it.elementType == MvElementTypes.COMMA }
            ?.delete()

        // remove previous comma if this is last element
        val index = container.attrItemArgumentList.indexOf(argument)
        if (index == container.attrItemArgumentList.size - 1) {
            element.getPrevNonCommentSibling()
                ?.takeIf { it.elementType == MvElementTypes.COMMA }
                ?.delete()
        }
        argument.delete()
    }
}
