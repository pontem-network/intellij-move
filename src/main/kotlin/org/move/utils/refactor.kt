package org.move.utils

import com.intellij.psi.PsiElement
import org.move.ide.refactoring.isValidMoveVariableIdentifier
import org.move.lang.MoveElementTypes
import org.move.lang.core.psi.MovePsiFactory
import org.move.lang.core.psi.ext.elementType

fun doRenameIdentifier(identifier: PsiElement, newName: String) {
    val factory = MovePsiFactory(identifier.project)
    val newIdentifier = when (identifier.elementType) {
        MoveElementTypes.IDENTIFIER -> {
            if (!isValidMoveVariableIdentifier(newName)) return
            factory.createIdentifier(newName)
        }
        else -> error("Unsupported identifier type for `$newName` (${identifier.elementType})")
    }
    identifier.replace(newIdentifier)
}
