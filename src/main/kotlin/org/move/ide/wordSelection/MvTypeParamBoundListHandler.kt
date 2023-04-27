package org.move.ide.wordSelection

import com.intellij.codeInsight.editorActions.ExtendWordSelectionHandlerBase
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import org.move.lang.core.psi.MvTypeParamBound
import org.move.lang.core.psi.ext.endOffset
import org.move.lang.core.psi.ext.startOffset

class MvTypeParamBoundListHandler : ExtendWordSelectionHandlerBase() {
    override fun canSelect(e: PsiElement): Boolean = e is MvTypeParamBound

    override fun select(
        e: PsiElement,
        editorText: CharSequence,
        cursorOffset: Int,
        editor: Editor
    ): List<TextRange>? {
        val typeParamBound = e as? MvTypeParamBound ?: return null
        val first = typeParamBound.abilityList.firstOrNull() ?: return null
        val last = typeParamBound.abilityList.lastOrNull() ?: return null
        return listOf(TextRange(first.startOffset, last.endOffset))
    }
}
