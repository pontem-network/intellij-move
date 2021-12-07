package org.move.ide.navigation

import com.intellij.psi.PsiFile
import com.intellij.psi.PsiRecursiveVisitor
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.functionSignatures
import org.move.lang.core.psi.ext.structSignatures
import org.move.lang.core.resolve.ref.Visibility

abstract class MvNamedElementsVisitor : MvVisitor(), PsiRecursiveVisitor {
    override fun visitFile(file: PsiFile) {
        file.acceptChildren(this)
    }

    override fun visitAddressDef(o: MvAddressDef) {
        o.addressBlock?.moduleDefList?.map { it.accept(this) }
    }

    override fun visitModuleDef(o: MvModuleDef) {
        processNamedElement(o)

        val functionSignatures = o.functionSignatures(Visibility.Internal())
        functionSignatures.map { it.accept(this) }

        val structSignatures = o.structSignatures()
        structSignatures.map { it.accept(this) }
    }

    override fun visitFunctionSignature(o: MvFunctionSignature) {
        processNamedElement(o)
    }

    override fun visitStructSignature(o: MvStructSignature) {
        processNamedElement(o)
    }

    abstract fun processNamedElement(element: MvNamedElement)
}
