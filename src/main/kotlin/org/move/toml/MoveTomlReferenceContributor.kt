package org.move.toml

import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiReferenceRegistrar

class MoveTomlReferenceContributor: PsiReferenceContributor() {
    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerReferenceProvider(
            MoveTomlPsiPatterns.dependencyLocal(), MoveTomlLocalPathReferenceProvider())
        registrar.registerReferenceProvider(
            MoveTomlPsiPatterns.dependencyGitUrl(), MoveTomlUrlReferenceProvider())
    }
}
