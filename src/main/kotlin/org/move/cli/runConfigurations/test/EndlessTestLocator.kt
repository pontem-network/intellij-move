package org.move.cli.runConfigurations.test

import com.intellij.execution.Location
import com.intellij.execution.PsiLocation
import com.intellij.execution.testframework.sm.runner.SMTestLocator
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import org.move.lang.core.psi.MvFunction
import org.move.lang.core.resolve.ref.NAMES
import org.move.lang.core.resolve.ref.Ns
import org.move.lang.core.types.fqName
import org.move.lang.index.MvNamedItemFilesIndex
import org.move.lang.moveProject

object EndlessTestLocator: SMTestLocator {

    private const val NAME_SEPARATOR: String = "::"
    private const val TEST_PROTOCOL: String = "endless:test"

    override fun getLocation(
        protocol: String,
        path: String,
        project: Project,
        scope: GlobalSearchScope
    ): List<Location<out PsiElement>> {
        if (protocol != TEST_PROTOCOL) return emptyList()
        val qualifiedName = path.trim()
        return buildList {
            val name = qualifiedName.substringAfterLast(NAME_SEPARATOR)
            for (entry in MvNamedItemFilesIndex.getEntriesFor(project, scope, listOf(name), NAMES)) {
                val element = entry.element()
                if (element is MvFunction) {
                    if (element.fqName()?.numericAddressText(element.moveProject) == qualifiedName) {
                        add(PsiLocation.fromPsiElement(element))
                    }
                }
            }
        }
    }

    fun getTestUrl(testName: String): String {
        return "$TEST_PROTOCOL://$testName"
    }
}
