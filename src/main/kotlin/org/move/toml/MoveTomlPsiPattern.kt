package org.move.toml

import com.intellij.patterns.PsiElementPattern
import com.intellij.patterns.VirtualFilePattern
import com.intellij.psi.PsiElement
import org.move.cli.MvConstants
import org.move.lang.core.psiElement
import org.toml.lang.psi.TomlKeySegment

object MoveTomlPsiPattern {
    private inline fun <reified I : PsiElement> cargoTomlPsiElement(): PsiElementPattern.Capture<I> {
        return psiElement<I>().inVirtualFile(
            VirtualFilePattern().withName(MvConstants.MANIFEST_FILE)
        )
    }

    /** Any element inside any TomlKey in Move.toml */
    val inKey: PsiElementPattern.Capture<PsiElement> =
        cargoTomlPsiElement<PsiElement>()
            .withParent(TomlKeySegment::class.java)
}
