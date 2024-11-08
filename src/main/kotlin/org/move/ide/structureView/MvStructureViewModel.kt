package org.move.ide.structureView

import com.intellij.icons.AllIcons
import com.intellij.ide.structureView.StructureViewModel
import com.intellij.ide.structureView.StructureViewModelBase
import com.intellij.ide.structureView.StructureViewTreeElement
import com.intellij.ide.util.treeView.smartTree.*
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiFile
import com.intellij.util.PlatformIcons
import org.move.lang.MoveFile
import org.move.lang.core.psi.MvNamedElement

class MvStructureViewModel(editor: Editor?, moveFile: MoveFile):
    StructureViewModelBase(
        moveFile,
        editor,
        MvStructureViewTreeElement(moveFile)
    ),
    StructureViewModel.ElementInfoProvider {

    init {
        withSuitableClasses(MvNamedElement::class.java)
        withSorters(ItemTypeSorter(), Sorter.ALPHA_SORTER)
    }

    override fun getFilters(): Array<Filter> =
        arrayOf(
            HidePrivateFunctionsFilter(),
            HideTestFunctionsFilter(),
            HideTestOnlyItemsFilter()
        )

    override fun isAlwaysShowsPlus(element: StructureViewTreeElement): Boolean = false

    override fun isAlwaysLeaf(element: StructureViewTreeElement): Boolean = element is PsiFile
}

class HidePrivateFunctionsFilter: Filter {
    override fun isVisible(treeNode: TreeElement): Boolean {
        // if action is set, only public items are visible
        return (treeNode as? MvStructureViewTreeElement)?.isPublicItem != false
    }

    @Suppress("DialogTitleCapitalization")
    override fun getPresentation(): ActionPresentation =
        ActionPresentationData("Hide private functions", null, PlatformIcons.PRIVATE_ICON)

    override fun getName() = ID

    override fun isReverted() = false

    companion object {
        const val ID = "MOVE_HIDE_PRIVATE_FUNCTIONS"
    }
}

class HideTestFunctionsFilter: Filter {
    override fun isVisible(treeNode: TreeElement): Boolean {
        // if action is set, only non-#[test] items are visible
        return (treeNode as? MvStructureViewTreeElement)?.isTestFunction?.not() ?: true
    }

    @Suppress("DialogTitleCapitalization")
    override fun getPresentation(): ActionPresentation =
        ActionPresentationData("Hide #[test] functions", null, AllIcons.Nodes.Test)

    override fun getName() = ID

    override fun isReverted() = false

    companion object {
        const val ID = "MOVE_HIDE_TEST_FUNCTIONS"
    }
}

class HideTestOnlyItemsFilter: Filter {
    override fun isVisible(treeNode: TreeElement): Boolean {
        // if action is set, only non-#[test_only] items are visible
        return (treeNode as? MvStructureViewTreeElement)?.isTestOnlyItem?.not() ?: true
    }

    @Suppress("DialogTitleCapitalization")
    override fun getPresentation(): ActionPresentation =
        ActionPresentationData("Hide #[test_only] items", null, AllIcons.Nodes.Type)

    override fun getName() = ID

    override fun isReverted() = false

    companion object {
        const val ID = "MOVE_HIDE_TEST_ONLY_ITEMS"
    }
}

