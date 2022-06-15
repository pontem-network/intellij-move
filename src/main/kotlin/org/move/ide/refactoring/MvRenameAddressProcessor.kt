package org.move.ide.refactoring

import com.intellij.psi.PsiElement
import com.intellij.refactoring.rename.RenamePsiElementProcessor
import org.move.lang.core.psi.ext.ancestorStrict
import org.toml.lang.psi.TomlKeySegment
import org.toml.lang.psi.TomlKeyValue
import org.toml.lang.psi.TomlTable

class MvRenameAddressProcessor : RenamePsiElementProcessor() {
    override fun canProcessElement(element: PsiElement): Boolean {
        if (element !is TomlKeySegment) return false
        if (element.containingFile.name != "Move.toml") return false
        val table = element.ancestorStrict<TomlKeyValue>()?.parent as? TomlTable
        return table != null
                && table.header.text == "[addresses]"
    }
}
