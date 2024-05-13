package org.move.lang.index

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.StringStubIndexExtension
import com.intellij.psi.stubs.StubIndex
import com.intellij.psi.stubs.StubIndexKey
import org.move.lang.core.psi.MvNamedElement
import org.move.lang.core.stubs.impl.MvFileStub
import org.move.openapiext.checkCommitIsNotInProgress

class MvNamedElementIndex : StringStubIndexExtension<MvNamedElement>() {
    override fun getVersion(): Int = MvFileStub.Type.stubVersion
    override fun getKey(): StubIndexKey<String, MvNamedElement> = KEY

    companion object {
        val KEY: StubIndexKey<String, MvNamedElement> =
            StubIndexKey.createIndexKey("org.move.index.NamedElementIndex")

        fun getAllKeys(project: Project): Collection<String> {
            checkCommitIsNotInProgress(project)
            return StubIndex.getInstance().getAllKeys(KEY, project)
        }

        fun processElementsByName(
            project: Project,
            target: String,
            scope: GlobalSearchScope = GlobalSearchScope.allScope(project),
            processor: (MvNamedElement) -> Boolean,
        ) {
            checkCommitIsNotInProgress(project)
            StubIndex.getInstance()
                .processElements(KEY, target, project, scope, MvNamedElement::class.java, processor)
        }

        fun getElementsByName(
            project: Project,
            name: String,
            scope: GlobalSearchScope,
        ): Collection<MvNamedElement> {
            checkCommitIsNotInProgress(project)
            return StubIndex.getElements(KEY, name, project, scope, MvNamedElement::class.java)
        }
    }
}
