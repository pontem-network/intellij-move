package org.move.lang.index

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.StringStubIndexExtension
import com.intellij.psi.stubs.StubIndex
import com.intellij.psi.stubs.StubIndexKey
import com.intellij.util.Processors
import org.move.lang.core.psi.MvFunction
import org.move.lang.core.stubs.impl.MvFileStub
import org.move.lang.core.types.ItemFQName
import org.move.openapiext.checkCommitIsNotInProgress
import org.move.openapiext.getElements

class MvEntryFunctionIndex : StringStubIndexExtension<MvFunction>() {
    override fun getKey() = KEY
    override fun getVersion(): Int = MvFileStub.Type.stubVersion

    companion object {
        val KEY: StubIndexKey<String, MvFunction> =
            StubIndexKey.createIndexKey("org.move.index.MvEntryFunctionIndex")

        fun getFunction(
            project: Project,
            qualName: String,
            scope: GlobalSearchScope = GlobalSearchScope.allScope(project)
        ): MvFunction? {
            checkCommitIsNotInProgress(project)
            val allFunctions = getElements(KEY, qualName, project, scope)
            return allFunctions.firstOrNull()
        }

        fun hasFunction(
            project: Project,
            qualName: String,
            scope: GlobalSearchScope = GlobalSearchScope.allScope(project)
        ): Boolean {
            checkCommitIsNotInProgress(project)
            val allFunctions = getElements(KEY, qualName, project, scope)
            return allFunctions.isNotEmpty()
        }

//        fun findFunctionsByQualName(
//            project: Project,
//            targetFQName: String,
//            scope: GlobalSearchScope = GlobalSearchScope.allScope(project)
//        ): Collection<MvFunction> {
//            checkCommitIsNotInProgress(project)
//            return getElements(KEY, targetFQName, project, scope)
//        }

        fun getAllKeysForCompletion(
            project: Project,
            scope: GlobalSearchScope = GlobalSearchScope.allScope(project)
        ): Collection<String> {
            val keys = getAllKeys(project, scope)
            return keys.mapNotNull {
                ItemFQName.fromCmdText(it)?.completionText()
            }
        }

        fun getAllKeys(
            project: Project,
            scope: GlobalSearchScope = GlobalSearchScope.allScope(project)
        ): Collection<String> {
            checkCommitIsNotInProgress(project)
            val allKeys = hashSetOf<String>()
            StubIndex.getInstance().processAllKeys(
                KEY,
                Processors.cancelableCollectProcessor(allKeys),
                scope
            )
            return allKeys
        }
    }
}
