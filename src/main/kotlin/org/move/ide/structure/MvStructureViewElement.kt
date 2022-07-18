package org.move.ide.structure

import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.structureView.StructureViewTreeElement
import com.intellij.ide.util.treeView.smartTree.TreeElement
import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.ui.Queryable
import com.intellij.psi.NavigatablePsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.move.lang.MoveFile
import org.move.lang.core.psi.MvAddressDef
import org.move.lang.core.psi.MvModule
import org.move.lang.core.psi.ext.functions
import org.move.lang.core.psi.ext.modules
import org.move.lang.core.resolve.ref.Visibility
import org.move.openapiext.common.isUnitTestMode

class MvStructureViewElement(
    val element: NavigatablePsiElement
) : StructureViewTreeElement, Queryable {
    override fun navigate(requestFocus: Boolean) {
        return element.navigate(requestFocus)
    }

    override fun canNavigate(): Boolean {
        return element.canNavigate()
    }

    override fun canNavigateToSource(): Boolean {
        return element.canNavigateToSource()
    }

    override fun getPresentation(): ItemPresentation {
        return this.element.presentation ?: PresentationData()
    }

    override fun getChildren(): Array<TreeElement> {
        return when (element) {
            is MoveFile -> {
                val elements =
                    PsiTreeUtil
                        .getChildrenOfTypeAsList(element, MvModule::class.java)
                        .toMutableList<NavigatablePsiElement>()
                for (addressBlock in element.addressBlocks()) {
                    elements.addAll(addressBlock.moduleList)
                }
                for (scriptBlock in element.scriptBlocks()) {
                    elements.addAll(scriptBlock.functionList)
                }
                elements.map { MvStructureViewElement(it) }.toTypedArray()
            }
            is MvAddressDef -> {
                val modules = element.modules()
                modules.map { MvStructureViewElement(it) }.toTypedArray()
            }
            is MvModule -> {
                val allFunctions = element.functions(Visibility.Internal)
                allFunctions.map { MvStructureViewElement(it) }.toTypedArray()
            }
            else -> emptyArray()
        }
    }

    override fun getValue(): Any {
        return this.element
    }

    // Used in `RsStructureViewTest`
    override fun putInfo(info: MutableMap<in String, in String>) {
        if (!isUnitTestMode) return
        val presentation = presentation
        info[NAME_KEY] = presentation.presentableText ?: ""
    }

    companion object {
        const val NAME_KEY: String = "name"
    }
}
