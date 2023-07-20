package org.move.ide.structureView

import com.intellij.ide.structureView.StructureViewModel
import com.intellij.ide.structureView.StructureViewModelBase
import com.intellij.ide.structureView.StructureViewTreeElement
import com.intellij.ide.util.treeView.smartTree.Sorter
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiFile
import org.move.lang.MoveFile
import org.move.lang.core.psi.MvNamedElement

class MvStructureViewModel(editor: Editor?, moveFile: MoveFile) :
    StructureViewModelBase(
        moveFile,
        editor,
        MvStructureViewTreeElement(moveFile)
    ),
    StructureViewModel.ElementInfoProvider {

    init {
        withSuitableClasses(MvNamedElement::class.java)
        withSorters(Sorter.ALPHA_SORTER)
    }

    override fun isAlwaysShowsPlus(element: StructureViewTreeElement): Boolean = false

    override fun isAlwaysLeaf(element: StructureViewTreeElement): Boolean = element is PsiFile
}
