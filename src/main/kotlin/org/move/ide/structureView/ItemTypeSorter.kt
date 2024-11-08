package org.move.ide.structureView

import com.intellij.icons.AllIcons
import com.intellij.ide.util.treeView.smartTree.ActionPresentation
import com.intellij.ide.util.treeView.smartTree.ActionPresentationData
import com.intellij.ide.util.treeView.smartTree.Sorter
import org.move.lang.core.psi.*

class ItemTypeComparator: Comparator<MvStructureViewTreeElement> {
    override fun compare(left: MvStructureViewTreeElement, right: MvStructureViewTreeElement): Int {
        val leftKind = getIntKind(left)
        val rightKind = getIntKind(right)
        return leftKind.compareTo(rightKind)
    }

    private fun getIntKind(treeElement: MvStructureViewTreeElement): Int {
        val psi = treeElement.psi
        return when (psi) {
            is MvConst -> 0
            is MvStruct -> 1
            is MvFunction -> 2
            is MvSchema -> 3
            is MvSpecFunction -> 4
            else -> 5
        }
    }
}

class ItemTypeSorter: Sorter {
    override fun getComparator(): Comparator<*> = ItemTypeComparator()

    override fun getPresentation(): ActionPresentation =
        ActionPresentationData("By Type", null, AllIcons.ObjectBrowser.SortByType)

    override fun isVisible(): Boolean = true

    override fun getName(): String = KIND_SORTER_ID

    companion object {
        const val KIND_SORTER_ID = "KIND_SORTER"
    }
}