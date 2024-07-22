package org.move.lang.core.psi.ext

import com.intellij.psi.PsiComment
import com.intellij.psi.SyntaxTraverser
import org.move.lang.core.psi.MvUseGroup
import org.move.lang.core.psi.MvUseSpeck

val MvUseGroup.names get() = this.useSpeckList.mapNotNull { it.path.identifier?.text }

//val MvUseItemGroup.parentUseSpeck: MvItemUseSpeck get() = parent as MvItemUseSpeck

val MvUseGroup.asTrivial: MvUseSpeck?
    get() {
        val speck = useSpeckList.singleOrNull() ?: return null
        // Do not change use-groups with comments
        if (SyntaxTraverser.psiTraverser(this).traverse().any { it is PsiComment }) return null
        return speck
    }
