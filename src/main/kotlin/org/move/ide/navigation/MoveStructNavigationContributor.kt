package org.move.ide.navigation

import com.intellij.navigation.ChooseByNameContributorEx
import com.intellij.navigation.NavigationItem
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.Processor
import com.intellij.util.indexing.FindSymbolParameters
import com.intellij.util.indexing.IdFilter
import org.move.lang.core.psi.MoveNamedElement
import org.move.lang.core.psi.MoveStructSignature
import org.move.openapiext.allMoveFilesForContentRoots

class MoveStructNavigationContributor : ChooseByNameContributorEx {
    override fun processNames(processor: Processor<in String>, scope: GlobalSearchScope, filter: IdFilter?) {
        // get all names
        val project = scope.project ?: return
        val visitor = object : MoveNamedElementsVisitor() {
            override fun processNamedElement(element: MoveNamedElement) {
                if (element is MoveStructSignature) {
                    val elementName = element.name ?: return
                    processor.process(elementName)
                }
            }
        }
        project.allMoveFilesForContentRoots().map { it.accept(visitor) }
    }

    override fun processElementsWithName(
        name: String,
        processor: Processor<in NavigationItem>,
        parameters: FindSymbolParameters
    ) {
        val project = parameters.project
        val visitor = object : MoveNamedElementsVisitor() {
            override fun processNamedElement(element: MoveNamedElement) {
                if (element is MoveStructSignature) {
                    val elementName = element.name ?: return
                    if (elementName == name) processor.process(element)
                }
            }
        }
        project.allMoveFilesForContentRoots().map { it.accept(visitor) }
    }
}
