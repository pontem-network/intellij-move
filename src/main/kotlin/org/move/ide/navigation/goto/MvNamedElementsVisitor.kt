package org.move.ide.navigation.goto

import com.intellij.psi.PsiFile
import com.intellij.psi.PsiRecursiveVisitor
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.*

abstract class MvNamedElementsVisitor : MvVisitor(), PsiRecursiveVisitor {

    override fun visitFile(file: PsiFile) = file.acceptChildren(this)

    override fun visitAddressDef(o: MvAddressDef) {
        o.modules().forEach { it.accept(this) }
    }

    override fun visitModule(o: MvModule) {
        processNamedElement(o)
        o.allFunctions().forEach { it.accept(this) }
        o.specFunctions().forEach { it.accept(this) }
        o.structs().forEach { it.accept(this) }
        o.constList.forEach { it.accept(this) }
    }

    override fun visitFunction(o: MvFunction) = processNamedElement(o)
    override fun visitSpecFunction(o: MvSpecFunction) = processNamedElement(o)

    override fun visitStruct(o: MvStruct) {
        processNamedElement(o)
        o.namedFields.forEach { processNamedElement(it) }
    }

    override fun visitConst(o: MvConst) = processNamedElement(o)

    abstract fun processNamedElement(element: MvNamedElement)
}
