package org.move.lang.index

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.StringStubIndexExtension
import com.intellij.psi.stubs.StubIndexKey
import org.move.lang.core.psi.MvModuleSpec
import org.move.lang.core.stubs.impl.MvFileStub
import org.move.openapiext.checkCommitIsNotInProgress
import org.move.openapiext.getElements

class MvModuleSpecStubIndex : StringStubIndexExtension<MvModuleSpec>() {
    override fun getVersion(): Int = MvFileStub.Type.stubVersion
    override fun getKey(): StubIndexKey<String, MvModuleSpec> = KEY

    companion object {
        val KEY: StubIndexKey<String, MvModuleSpec> =
            StubIndexKey.createIndexKey("org.move.index.ModuleSpecIndex")

        fun getElementsByModuleName(
            project: Project,
            moduleFqName: String,
            scope: GlobalSearchScope = GlobalSearchScope.allScope(project)
        ): Collection<MvModuleSpec> {
            checkCommitIsNotInProgress(project)
            return getElements(KEY, moduleFqName, project, scope)
        }
    }
}
