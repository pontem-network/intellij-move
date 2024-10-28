package org.move.utils

import com.intellij.psi.PsiElement
import org.move.ide.refactoring.isValidMoveVariableIdentifier
import org.move.lang.MvElementTypes
import org.move.lang.MvElementTypes.QUOTE_IDENTIFIER
import org.move.lang.core.psi.MvPsiFactory
import org.move.lang.core.psi.ext.elementType

fun doRenameIdentifier(identifier: PsiElement, newName: String) {
    val factory = MvPsiFactory(identifier.project)
    val newIdentifier = when (identifier.elementType) {
        MvElementTypes.IDENTIFIER -> {
            if (!isValidMoveVariableIdentifier(newName)) return
            factory.identifier(newName)
        }
        QUOTE_IDENTIFIER -> factory.quoteIdentifier(newName)
        else -> error("Unsupported identifier type for `$newName` (${identifier.elementType})")
    }
    identifier.replace(newIdentifier)
}
