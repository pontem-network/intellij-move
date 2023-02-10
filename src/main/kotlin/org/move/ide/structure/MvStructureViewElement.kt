package org.move.ide.structure

import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.structureView.StructureViewTreeElement
import com.intellij.ide.util.treeView.smartTree.TreeElement
import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.ui.Queryable
import com.intellij.psi.NavigatablePsiElement
import org.move.lang.MoveFile
import org.move.lang.core.psi.MvAddressDef
import org.move.lang.core.psi.MvModule
import org.move.lang.core.psi.ext.allFunctions
import org.move.lang.core.psi.ext.modules
import org.move.lang.core.psi.ext.structs
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
        val items = when (element) {
            is MoveFile -> {
                listOf(
                    element.modules().toList(),
                    element.scriptBlocks().flatMap { it.functionList }
                ).flatten()
            }
            is MvAddressDef -> element.modules()
            is MvModule -> {
                listOf(
                    element.structs(),
                    element.allFunctions()
                ).flatten()
            }
            else -> emptyList()
        }
        return items.map { MvStructureViewElement(it) }.toTypedArray()
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
