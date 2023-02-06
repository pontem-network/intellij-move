package org.move.lang.core.psi.impl

import com.intellij.extapi.psi.StubBasedPsiElementBase
import com.intellij.ide.projectView.PresentationData
import com.intellij.lang.ASTNode
import com.intellij.navigation.ItemPresentation
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.stubs.StubElement
import org.move.lang.MvElementTypes.IDENTIFIER
import org.move.lang.core.psi.MvElement
import org.move.lang.core.psi.MvNameIdentifierOwner
import org.move.lang.core.psi.MvPsiFactory
import org.move.lang.core.psi.containingModule
import org.move.lang.core.stubs.impl.MvNamedStub

abstract class MvNameIdentifierOwnerImpl(node: ASTNode) : MvNamedElementImpl(node),
                                                          MvNameIdentifierOwner {
    override fun getNameIdentifier(): PsiElement? = nameElement
}
