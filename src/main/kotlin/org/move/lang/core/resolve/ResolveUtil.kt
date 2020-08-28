package org.move.lang.core.resolve

import com.intellij.psi.PsiElement

object ResolveUtil {
    fun getResolveScopeFor(element: PsiElement): MoveResolveScope? {
        var current = element.parent
        while (current != null) {
            when (current) {
                is MoveResolveScope -> return current
                else -> current = current.parent
            }
        }
        return null
    }
}