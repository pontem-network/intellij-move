package org.move.lang.index

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.StringStubIndexExtension
import com.intellij.psi.stubs.StubIndex
import com.intellij.psi.stubs.StubIndexKey
import com.intellij.util.Processors
import org.move.cli.MoveProject
import org.move.lang.core.psi.MvFunction
import org.move.lang.core.psi.ext.isEntry
import org.move.lang.core.psi.ext.isView
import org.move.lang.core.stubs.impl.MvFileStub
import org.move.lang.core.types.ItemQualName
import org.move.openapiext.checkCommitIsNotInProgress
import org.move.openapiext.getElements

class MvViewFunctionIndex : StringStubIndexExtension<MvFunction>() {
    override fun getKey() = KEY
    override fun getVersion(): Int = MvFileStub.Type.stubVersion

    companion object {
        val KEY: StubIndexKey<String, MvFunction> =
            StubIndexKey.createIndexKey("org.move.index.MvViewFunctionIndex")

        fun getViewFunction(
            project: Project,
            functionId: String,
            scope: GlobalSearchScope = GlobalSearchScope.allScope(project)
        ): MvFunction? {
            return getFunction(project, functionId, scope) { it.isView }
        }

        fun getFunctionByFunctionId(
            moveProject: MoveProject,
            functionId: String,
            scope: GlobalSearchScope = GlobalSearchScope.allScope(moveProject.project),
            itemFilter: (MvFunction) -> Boolean = { _ -> true }
        ): MvFunction? {
            val project = moveProject.project
            checkCommitIsNotInProgress(project)

            return getFunction(project, functionId, scope, itemFilter)
                ?: run {
                    val addressValue = ItemQualName.split(functionId)?.first ?: return@run null
                    val namedAddresses = moveProject.getAddressNamesForValue(addressValue)
                    namedAddresses
                        .map {
                            val modifiedFunctionId = functionId.replace(addressValue, it)
                            getFunction(project, modifiedFunctionId, scope, itemFilter)
                        }
                        .firstOrNull()
                }
        }

        private fun getFunction(
            project: Project,
            functionId: String,
            scope: GlobalSearchScope = GlobalSearchScope.allScope(project),
            itemFilter: (MvFunction) -> Boolean = { _ -> true }
        ): MvFunction? {
            checkCommitIsNotInProgress(project)
            val allFunctions = getElements(KEY, functionId, project, scope)
            return allFunctions.firstOrNull(itemFilter)
        }

        fun getAllKeysForCompletion(
            project: Project,
            scope: GlobalSearchScope = GlobalSearchScope.allScope(project)
        ): Collection<String> {
            val keys = getAllKeys(project, scope)
            return keys.mapNotNull { ItemQualName.qualNameForCompletion(it) }
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
