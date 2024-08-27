package org.move.ide.structureView

import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.structureView.StructureViewTreeElement
import com.intellij.ide.util.treeView.smartTree.TreeElement
import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.ui.Queryable
import com.intellij.psi.NavigatablePsiElement
import org.move.lang.MoveFile
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.*
import org.move.openapiext.common.isUnitTestMode

class MvStructureViewTreeElement(val element: NavigatablePsiElement): StructureViewTreeElement,
                                                                      Queryable {
    val isPublic: Boolean
        get() {
            return when (element) {
                is MvFunction -> element.isPublic
                is MvConst -> false
                else -> true
            }
        }

    val isTestFunction: Boolean
        get() =
            (element as? MvFunction)?.hasTestAttr ?: false

    val isTestOnlyItem: Boolean
        get() =
            (element as? MvDocAndAttributeOwner)?.hasTestOnlyAttr ?: false

    override fun navigate(requestFocus: Boolean) = element.navigate(requestFocus)
    override fun canNavigate(): Boolean = element.canNavigate()
    override fun canNavigateToSource(): Boolean = element.canNavigateToSource()
    override fun getPresentation(): ItemPresentation = this.element.presentation ?: PresentationData()

    override fun getChildren(): Array<TreeElement> {
        val items = when (element) {
            is MoveFile -> {
                listOf(
                    element.modules().toList(),
                    element.scripts().flatMap { it.functionList }
                ).flatten()
            }
            is MvAddressDef -> element.modules()
            is MvModule -> {
                listOf(
                    element.consts(),
                    element.structs(),
                    element.allFunctions(),
                    element.specFunctions(),
                ).flatten()
            }
            is MvStruct -> element.namedFields
            else -> emptyList()
        }
        return items.map { MvStructureViewTreeElement(it) }.toTypedArray()
    }

    override fun getValue(): Any = this.element

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
