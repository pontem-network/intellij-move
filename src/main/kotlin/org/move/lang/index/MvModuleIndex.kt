package org.move.lang.index

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.StringStubIndexExtension
import com.intellij.psi.stubs.StubIndex
import com.intellij.psi.stubs.StubIndexEx
import com.intellij.psi.stubs.StubIndexKey
import com.intellij.util.indexing.*
import org.move.lang.core.psi.MvModule
import org.move.lang.core.stubs.impl.MvFileStub
import org.move.openapiext.checkCommitIsNotInProgress

class MvModuleIndex: StringStubIndexExtension<MvModule>() {
    override fun getVersion(): Int = MvFileStub.Type.stubVersion
    override fun getKey(): StubIndexKey<String, MvModule> = KEY

    companion object {
        val KEY: StubIndexKey<String, MvModule> =
            StubIndexKey.createIndexKey("org.move.index.ModuleIndex")

        fun getAllModuleNames(project: Project): Collection<String> {
            checkCommitIsNotInProgress(project)
            return StubIndex.getInstance().getAllKeys(KEY, project)
        }

        fun processModulesByName(
            project: Project,
            target: String,
            scope: GlobalSearchScope = GlobalSearchScope.allScope(project),
            processor: (MvModule) -> Boolean,
        ) {
            checkCommitIsNotInProgress(project)
            StubIndex.getInstance()
                .processElements(KEY, target, project, scope, MvModule::class.java, processor)
        }

        fun getModulesByName(
            project: Project,
            name: String,
            scope: GlobalSearchScope,
        ): Collection<MvModule> {
            checkCommitIsNotInProgress(project)
            return StubIndex.getElements(KEY, name, project, scope, MvModule::class.java)
        }

//        fun getModuleByName(
//            project: Project,
//            name: String,
//            scope: GlobalSearchScope,
//        ): MvModule? {
//            return getModulesByName(project, name, scope).singleOrNull()
//        }
    }
}