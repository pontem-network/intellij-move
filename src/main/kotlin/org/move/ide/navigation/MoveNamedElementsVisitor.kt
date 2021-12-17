package org.move.ide.navigation

import com.intellij.psi.PsiFile
import com.intellij.psi.PsiRecursiveVisitor
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.functions
import org.move.lang.core.psi.ext.structs
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

        val functionSignatures = o.functions(Visibility.Internal)
        functionSignatures.map { it.accept(this) }

        val structSignatures = o.structs()
        structSignatures.map { it.accept(this) }
    }

    override fun visitFunction(o: MvFunction) = processNamedElement(o)

    override fun visitStruct_(o: MvStruct_) = processNamedElement(o)

    abstract fun processNamedElement(element: MvNamedElement)
}
