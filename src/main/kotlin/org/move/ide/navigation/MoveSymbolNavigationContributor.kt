package org.move.ide.navigation

import com.intellij.navigation.ChooseByNameContributorEx
import com.intellij.navigation.NavigationItem
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.Processor
import com.intellij.util.indexing.FindSymbolParameters
import com.intellij.util.indexing.IdFilter
import org.move.lang.core.psi.MvNamedElement
import org.move.openapiext.allMoveFiles


class MvSymbolNavigationContributor : ChooseByNameContributorEx {
    override fun processNames(
        processor: Processor<in String>,
        scope: GlobalSearchScope,
        filter: IdFilter?
    ) {
        // get all names
        val project = scope.project ?: return
        val visitor = object : MvNamedElementsVisitor() {
            override fun processNamedElement(element: MvNamedElement) {
                val elementName = element.name ?: return
                processor.process(elementName)
            }
        }
        project.allMoveFiles().map { it.accept(visitor) }
    }

    override fun processElementsWithName(
        name: String,
        processor: Processor<in NavigationItem>,
        parameters: FindSymbolParameters
    ) {
        val project = parameters.project
        val visitor = object : MvNamedElementsVisitor() {
            override fun processNamedElement(element: MvNamedElement) {
                val elementName = element.name ?: return
                if (elementName == name) processor.process(element)
            }
        }
        project.allMoveFiles().map { it.accept(visitor) }
    }
}
