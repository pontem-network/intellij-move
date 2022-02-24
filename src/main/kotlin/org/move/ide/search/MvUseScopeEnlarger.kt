package org.move.ide.search

import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScopes
import com.intellij.psi.search.SearchScope
import com.intellij.psi.search.UseScopeEnlarger
import org.move.cli.GlobalScope
import org.move.lang.moveProject
import org.move.openapiext.findVirtualFile

class MvUseScopeEnlarger : UseScopeEnlarger() {
    override fun getAdditionalUseScope(element: PsiElement): SearchScope? {
        val project = element.project
        val moveProject = element.moveProject ?: return null
        val dirs = listOf(
            moveProject.moduleFolders(GlobalScope.DEV),
            listOfNotNull(
                moveProject.testsDir()?.findVirtualFile(),
                moveProject.scriptsDir()?.findVirtualFile()
            ),
        ).flatten()
        return GlobalSearchScopes.directoriesScope(
            project, true, *dirs.toTypedArray()
        )
    }
}
