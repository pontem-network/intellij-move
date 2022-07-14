package org.move.ide.navigation

import com.intellij.psi.PsiFile
import com.intellij.psi.PsiRecursiveVisitor
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.allFunctions
import org.move.lang.core.psi.ext.constBindings
import org.move.lang.core.psi.ext.consts
import org.move.lang.core.psi.ext.structs

abstract class MvNamedElementsVisitor : MvVisitor(), PsiRecursiveVisitor {

    override fun visitFile(file: PsiFile) = file.acceptChildren(this)

    override fun visitAddressDef(o: MvAddressDef) {
        o.addressBlock?.moduleList?.map { it.accept(this) }
    }

    override fun visitModule(o: MvModule) {
        processNamedElement(o)

        o.allFunctions().forEach { it.accept(this) }
        o.structs().forEach { it.accept(this) }
        o.constBindings().forEach { it.accept(this) }
    }

    override fun visitFunction(o: MvFunction) = processNamedElement(o)

    override fun visitStruct(o: MvStruct) = processNamedElement(o)

    override fun visitBindingPat(o: MvBindingPat) = processNamedElement(o)

    abstract fun processNamedElement(element: MvNamedElement)
}
