package org.move.ide.navigation

import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.structureView.*
import com.intellij.ide.util.treeView.smartTree.TreeElement
import com.intellij.lang.PsiStructureViewFactory
import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.ui.Queryable
import com.intellij.psi.NavigatablePsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import org.move.lang.MoveFile
import org.move.lang.core.psi.MoveAddressDef
import org.move.lang.core.psi.MoveModuleDef
import org.move.lang.core.psi.ext.functionSignatures
import org.move.lang.core.psi.ext.modules
import org.move.lang.core.resolve.ref.Visibility
import org.move.openapiext.common.isUnitTestMode

class MoveStructureViewElement(val element: NavigatablePsiElement) : StructureViewTreeElement, Queryable {
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
                    PsiTreeUtil.getChildrenOfTypeAsList(element, MoveModuleDef::class.java)
                    .toMutableList<NavigatablePsiElement>()
                for (addressBlock in element.addressBlocks()) {
                    elements.addAll(addressBlock.moduleDefList)
                }
                for (scriptBlock in element.scriptBlocks()) {
                    elements.addAll(scriptBlock.functionDefList
                                        .mapNotNull { it.functionSignature })
                }
                elements.map { MoveStructureViewElement(it) }.toTypedArray()
            }
            is MoveAddressDef -> {
                val modules = element.modules()
                modules.map { MoveStructureViewElement(it) }.toTypedArray()
            }
            is MoveModuleDef -> {
                val allFunctions = element.functionSignatures(Visibility.Internal())
                allFunctions.map { MoveStructureViewElement(it) }.toTypedArray()
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

class MoveStructureViewModel(psiFile: PsiFile) :
    StructureViewModelBase(psiFile, MoveStructureViewElement(psiFile)),
    StructureViewModel.ElementInfoProvider {

    override fun isAlwaysShowsPlus(element: StructureViewTreeElement?): Boolean {
        return false
    }

    override fun isAlwaysLeaf(element: StructureViewTreeElement?): Boolean {
        return element is PsiFile
    }

}

class MoveStructureViewBuilderFactory : PsiStructureViewFactory {
    override fun getStructureViewBuilder(psiFile: PsiFile): StructureViewBuilder {
        return object : TreeBasedStructureViewBuilder() {
            override fun createStructureViewModel(editor: Editor?): StructureViewModel {
                return MoveStructureViewModel(psiFile)
            }
        }
    }
}
