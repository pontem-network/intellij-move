package org.move.ide.structureView

import com.intellij.ide.structureView.StructureViewTreeElement
import com.intellij.ide.util.treeView.smartTree.TreeElement
import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.ui.Queryable
import com.intellij.psi.NavigatablePsiElement
import com.intellij.psi.PsiElement
import com.intellij.util.containers.map2Array
import org.move.ide.presentation.getPresentationForStructure
import org.move.lang.MoveFile
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.*
import org.move.openapiext.common.isUnitTestMode

class MvStructureViewTreeElement(val psi: NavigatablePsiElement): StructureViewTreeElement,
                                                                  Queryable {

//    val psiAnchor = TreeAnchorizer.getService().createAnchor(element)
//    val psi: NavigatablePsiElement?
//        get() =
//            TreeAnchorizer.getService().retrieveElement(psiAnchor) as? NavigatablePsiElement

    val isPublicItem: Boolean =
        when (val psi = psi) {
            is MvFunction -> psi.isPublic
            is MvConst -> false
            else -> true
        }

    val isTestFunction: Boolean get() = (psi as? MvFunction)?.hasTestAttr == true

    val isTestOnlyItem: Boolean get() = (psi as? MvDocAndAttributeOwner)?.hasTestOnlyAttr == true

    override fun navigate(requestFocus: Boolean) = psi.navigate(requestFocus)
    override fun canNavigate(): Boolean = psi.canNavigate()
    override fun canNavigateToSource(): Boolean = psi.canNavigateToSource()
    override fun getValue(): PsiElement = psi

    override fun getPresentation(): ItemPresentation = psi.let(::getPresentationForStructure)

    override fun getChildren(): Array<out TreeElement?> =
        childElements.map2Array { MvStructureViewTreeElement(it) }

    private val childElements: List<NavigatablePsiElement>
        get() {
            return when (val psi = psi) {
                is MoveFile -> {
                    listOf(
                        psi.modules().toList(),
                        psi.scripts().flatMap { it.functionList }
                    ).flatten()
                }
                is MvAddressDef -> psi.modules()
                is MvModule -> {
                    listOf(
                        psi.constList,
                        psi.structs(),
                        psi.enumList,
                        psi.allFunctions(),
                        psi.specFunctions(),
                    ).flatten()
                }
                is MvFieldsOwner -> psi.namedFields
                is MvEnum -> psi.variants
                else -> emptyList()
            }
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
