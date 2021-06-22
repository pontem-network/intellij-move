package org.move.ide.navigation

import com.intellij.psi.PsiFile
import com.intellij.psi.PsiRecursiveVisitor
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.functionSignatures
import org.move.lang.core.psi.ext.structSignatures
import org.move.lang.core.resolve.ref.Visibility

abstract class MoveNamedElementsVisitor : MoveVisitor(), PsiRecursiveVisitor {
    override fun visitFile(file: PsiFile) {
        file.acceptChildren(this)
    }

    override fun visitAddressDef(o: MoveAddressDef) {
        o.addressBlock?.moduleDefList?.map { it.accept(this) }
    }

    override fun visitModuleDef(o: MoveModuleDef) {
        processNamedElement(o)

        val functionSignatures = o.functionSignatures(Visibility.Internal())
        functionSignatures.map { it.accept(this) }

        val structSignatures = o.structSignatures()
        structSignatures.map { it.accept(this) }
    }

    override fun visitFunctionSignature(o: MoveFunctionSignature) {
        processNamedElement(o)
    }

    override fun visitStructSignature(o: MoveStructSignature) {
        processNamedElement(o)
    }

    abstract fun processNamedElement(element: MoveNamedElement)
}
