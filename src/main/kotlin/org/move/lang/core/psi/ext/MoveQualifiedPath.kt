package org.move.lang.core.psi.ext

import com.intellij.psi.PsiElement
import org.move.lang.MoveElementTypes
import org.move.lang.MoveElementTypes.IDENTIFIER
import org.move.lang.core.psi.MoveQualifiedPath

val MoveQualifiedPath.identifierNameElement: PsiElement
    get() = checkNotNull(findLastChildByType(IDENTIFIER)) {
        "Path must contain identifier: $this `${this.text}` at `${this.containingFile.virtualFile.path}`"
    }

val MoveQualifiedPath.moduleNameElement: PsiElement?
    get() {
        val idents = childrenByType(IDENTIFIER).toList()
        if (idents.size < 2) {
            return null
        }
        return idents[0]
    }

val MoveQualifiedPath.addressElement: PsiElement?
    get() = childrenByType(MoveElementTypes.ADDRESS_LITERAL).firstOrNull()

val MoveQualifiedPath.identifierName: String
    get() = identifierNameElement.text

val MoveQualifiedPath.moduleName: String?
    get() = moduleNameElement?.text

val MoveQualifiedPath.address: String? get() = addressElement?.text
