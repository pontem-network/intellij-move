package org.move.toml

import com.intellij.psi.*
import com.intellij.util.ProcessingContext
import org.move.lang.core.MovePsiPatterns
import org.move.lang.core.psi.MoveNamedAddress
import org.move.lang.core.resolve.ref.NamedAddressReference

class NamedAddressReferenceProvider : PsiReferenceProvider() {
    override fun getReferencesByElement(
        element: PsiElement,
        context: ProcessingContext
    ): Array<PsiReference> {
        if (element !is MoveNamedAddress) return emptyArray()
        return arrayOf(NamedAddressReference(element))
    }
}

class NamedAddressReferenceContributor : PsiReferenceContributor() {
    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerReferenceProvider(
            MovePsiPatterns.namedAddress(), NamedAddressReferenceProvider()
        )
    }
}
