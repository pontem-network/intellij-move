package org.move.ide.refactoring.toml

import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.refactoring.rename.PsiElementRenameHandler
import org.move.openapiext.addressesTable
import org.toml.lang.psi.TomlKeySegment

class TomlRenameHandler : PsiElementRenameHandler() {
    override fun isAvailableOnDataContext(dataContext: DataContext): Boolean {
        val element = getElement(dataContext)
        val editor = CommonDataKeys.EDITOR.getData(dataContext)
        val file = CommonDataKeys.PSI_FILE.getData(dataContext)
        if (editor == null || file == null) return false
        return element is TomlKeySegment && element.addressesTable != null
    }
}
