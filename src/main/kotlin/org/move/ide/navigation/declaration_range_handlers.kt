package org.move.ide.navigation

import com.intellij.codeInsight.hint.DeclarationRangeHandler
import com.intellij.openapi.util.TextRange
import org.move.lang.core.psi.MoveFunctionDef
import org.move.lang.core.psi.MoveModuleDef
import org.move.lang.core.psi.ext.endOffset
import org.move.lang.core.psi.ext.startOffset

class ModuleDeclarationRangeHandler : DeclarationRangeHandler<MoveModuleDef> {
    override fun getDeclarationRange(container: MoveModuleDef): TextRange {
        val startOffset = container.startOffset
        val endOffset = container.moduleBlock?.endOffset ?: container.endOffset
        return TextRange(startOffset, endOffset)
    }
}

class FunctionDeclarationRangeHandler: DeclarationRangeHandler<MoveFunctionDef> {
    override fun getDeclarationRange(container: MoveFunctionDef): TextRange {
        return TextRange(container.startOffset,
                         container.codeBlock?.endOffset ?: container.endOffset)
    }
}
