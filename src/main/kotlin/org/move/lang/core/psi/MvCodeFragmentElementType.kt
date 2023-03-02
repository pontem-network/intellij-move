package org.move.lang.core.psi

import com.intellij.lang.ASTNode
import com.intellij.lang.PsiBuilderFactory
import com.intellij.psi.impl.source.tree.ICodeFragmentElementType
import com.intellij.psi.impl.source.tree.TreeElement
import com.intellij.psi.tree.IElementType
import org.move.lang.MoveLanguage
import org.move.lang.MoveParser
import org.move.lang.MvElementTypes

class MvCodeFragmentElementType(
    private val elementType: IElementType,
    debugName: String
) :
    ICodeFragmentElementType(debugName, MoveLanguage) {

    override fun parseContents(chameleon: ASTNode): ASTNode? {
        if (chameleon !is TreeElement) return null
        val project = chameleon.manager.project
        val builder = PsiBuilderFactory.getInstance().createBuilder(project, chameleon)
        val root = MoveParser().parse(elementType, builder)
        return root.firstChildNode
    }

    companion object {
        val QUAL_PATH_TYPE = MvCodeFragmentElementType(MvElementTypes.QUAL_PATH_CODE_FRAGMENT_ELEMENT, "MV_QUAL_PATH_CODE_FRAGMENT")
    }
}
