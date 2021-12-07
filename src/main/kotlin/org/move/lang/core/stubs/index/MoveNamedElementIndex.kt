package org.move.lang.core.stubs.index

import com.intellij.psi.stubs.StringStubIndexExtension
import com.intellij.psi.stubs.StubIndexKey
import org.move.lang.core.psi.MvNamedElement
import org.move.lang.core.stubs.MvFileStub

class MvNamedElementIndex : StringStubIndexExtension<MvNamedElement>() {
    override fun getVersion(): Int = MvFileStub.Type.stubVersion

    override fun getKey(): StubIndexKey<String, MvNamedElement> = KEY

    companion object {
        val KEY: StubIndexKey<String, MvNamedElement> =
            StubIndexKey.createIndexKey("org.move.lang.core.stubs.index.MvNamedElementIndex")

//        fun findElementsByName(
//            project: Project,
//            target: String,
//            scope: GlobalSearchScope = GlobalSearchScope.allScope(project),
//        ): Collection<MvNamedElement> {
//            checkCommitIsNotInProgress(project)
//            return getElements(KEY, target, project, scope)
//        }
    }
}
