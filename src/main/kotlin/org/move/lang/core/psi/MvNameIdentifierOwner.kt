package org.move.lang.core.psi

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner

interface MvNameIdentifierOwner : MvNamedElement,
                                  PsiNameIdentifierOwner


interface MvMandatoryNameIdentifierOwner : MvMandatoryNamedElement,
                                           PsiNameIdentifierOwner {

    override fun getName(): String

    override fun getNameIdentifier(): PsiElement
}
