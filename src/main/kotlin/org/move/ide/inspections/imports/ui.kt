/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.move.ide.inspections.imports

import com.intellij.codeInsight.navigation.NavigationUtil
import com.intellij.ide.util.DefaultPsiElementCellRenderer
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.PopupStep
import com.intellij.openapi.ui.popup.util.BaseListPopupStep
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.FakePsiElement
import com.intellij.ui.popup.list.ListPopupImpl
import com.intellij.ui.popup.list.PopupListElementRenderer
import org.jetbrains.annotations.TestOnly
import org.move.openapiext.common.isUnitTestMode
import java.awt.BorderLayout
import javax.swing.Icon
import javax.swing.JPanel
import javax.swing.ListCellRenderer

private var MOCK: ImportItemUi? = null

fun showItemsToImportChooser(
    project: Project,
    dataContext: DataContext,
    items: List<ImportCandidate>,
    callback: (ImportCandidate) -> Unit
) {
    val itemImportUi = if (isUnitTestMode) {
        MOCK
            ?: error(
                "Multiple items: ${items.map { it.qualName.editorText() }}. " +
                        "You should set mock ui via `withMockImportItemUi`"
            )
    } else {
        PopupImportItemUi(project, dataContext)
    }
    itemImportUi.chooseItem(items, callback)
}

@TestOnly
fun withMockImportItemUi(mockUi: ImportItemUi, action: () -> Unit) {
    MOCK = mockUi
    try {
        action()
    } finally {
        MOCK = null
    }
}

interface ImportItemUi {
    fun chooseItem(items: List<ImportCandidate>, callback: (ImportCandidate) -> Unit)
}

private class PopupImportItemUi(private val project: Project, private val dataContext: DataContext) :
    ImportItemUi {

    override fun chooseItem(items: List<ImportCandidate>, callback: (ImportCandidate) -> Unit) {
        val candidatePsiItems = items.map(::ImportCandidatePsiElement)

        // TODO: sort items in popup
        val step =
            object : BaseListPopupStep<ImportCandidatePsiElement>("Item to Import", candidatePsiItems) {
                override fun isAutoSelectionEnabled(): Boolean = false
                override fun isSpeedSearchEnabled(): Boolean = true
                override fun hasSubstep(selectedValue: ImportCandidatePsiElement?): Boolean = false

                override fun onChosen(
                    selectedValue: ImportCandidatePsiElement?,
                    finalChoice: Boolean
                ): PopupStep<*>? {
                    if (selectedValue == null) return PopupStep.FINAL_CHOICE
                    return doFinalStep { callback(selectedValue.importCandidate) }
                }

                override fun getTextFor(value: ImportCandidatePsiElement): String =
                    value.importCandidate.qualName.editorText()

                override fun getIconFor(value: ImportCandidatePsiElement): Icon =
                    value.importCandidate.element.getIcon(0)
            }

        @Suppress("UNCHECKED_CAST")
        val popup = object : ListPopupImpl(project, step) {
            override fun getListElementRenderer(): ListCellRenderer<*> {
                val baseRenderer = super.getListElementRenderer() as PopupListElementRenderer<Any>
                val psiRenderer = RsImportCandidateCellRenderer()
                return ListCellRenderer<Any> { list, value, index, isSelected, cellHasFocus ->
                    @Suppress("MissingAccessibleContext")
                    val panel = JPanel(BorderLayout())
                    baseRenderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
                    panel.add(baseRenderer.nextStepLabel, BorderLayout.EAST)
                    panel.add(
                        psiRenderer.getListCellRendererComponent(
                            list,
                            value,
                            index,
                            isSelected,
                            cellHasFocus
                        )
                    )
                    panel
                }
            }
        }
        NavigationUtil.hidePopupIfDumbModeStarts(popup, project)
        popup.showInBestPositionFor(dataContext)
    }
}

private class ImportCandidatePsiElement(val importCandidate: ImportCandidate) : FakePsiElement() {
    override fun getParent(): PsiElement? = importCandidate.element.parent
}

private class RsImportCandidateCellRenderer : DefaultPsiElementCellRenderer() {

    private val Any.importCandidate: ImportCandidate? get() = (this as? ImportCandidatePsiElement)?.importCandidate

    override fun getIcon(element: PsiElement): Icon =
        element.importCandidate?.element?.getIcon(iconFlags) ?: super.getIcon(element)

    override fun getElementText(element: PsiElement): String =
        element.importCandidate?.element?.name ?: super.getElementText(element)

    override fun getContainerText(element: PsiElement, name: String): String? {
        val importCandidate = element.importCandidate
        return if (importCandidate != null) {
            val fqName = importCandidate.qualName
            val container = if (fqName.moduleName == null) {
                fqName.address.text()
            } else {
                "${fqName.address.text()}::${fqName.moduleName}"
            }
            "($container)"
        } else {
            super.getContainerText(element, name)
        }
    }

//    override fun getItemLocation(value: Any?): TextWithIcon? {
//        // TODO: change into proper package name
//        val moveProject = value?.importCandidate?.element?.moveProject ?: return null
//        val packageName = moveProject.packageName ?: return null
//        return TextWithIcon(packageName, MvIcons.MOVE)
//    }
}
