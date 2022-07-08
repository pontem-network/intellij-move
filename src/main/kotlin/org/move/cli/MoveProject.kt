package org.move.cli

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.GlobalSearchScopes
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.util.containers.addIfNotNull
import org.move.cli.manifest.MoveToml
import org.move.lang.MoveFile
import org.move.lang.core.psi.ext.wrapWithList
import org.move.lang.core.types.Address
import org.move.lang.toMoveFile
import org.move.lang.toNioPathOrNull
import org.move.openapiext.common.checkUnitTestMode
import org.move.openapiext.contentRoots
import org.move.stdext.chain
import org.move.stdext.iterateMoveVirtualFiles
import java.nio.file.Path

data class MoveProject(
    val project: Project,
    val currentPackage: MovePackage,
    val dependencies: List<Pair<MovePackage, RawAddressMap>>
) : UserDataHolderBase() {

    val contentRoot: VirtualFile get() = this.currentPackage.contentRoot
    val contentRootPath: Path? get() = this.currentPackage.contentRoot.toNioPathOrNull()

    fun movePackages(): Sequence<MovePackage> {
        return currentPackage.wrapWithList()
            .chain(dependencies.map { it.first }.reversed())
    }

    fun sourceFolders(): List<VirtualFile> {
        val folders = mutableListOf<VirtualFile>()
        folders.addIfNotNull(currentPackage.sourcesFolder)
        folders.addIfNotNull(currentPackage.testsFolder)

        val depFolders = dependencies.asReversed().mapNotNull { it.first.sourcesFolder }
        folders.addAll(depFolders)
        return folders
    }

    fun addresses(): PackageAddresses {
        return CachedValuesManager.getManager(this.project).getCachedValue(this) {
            val packageName = currentPackage.packageName
            val cumulativeAddresses = PackageAddresses(mutableAddressMap(), placeholderMap())
            for ((depPackage, subst) in this.dependencies) {
                cumulativeAddresses.extendWith(depPackage.addresses())
                cumulativeAddresses.applySubstitution(subst, packageName)
            }
            cumulativeAddresses.extendWith(this.currentPackage.addresses())

            CachedValueProvider.Result.create(
                cumulativeAddresses,
                PsiModificationTracker.MODIFICATION_COUNT
            )
        }
    }

    fun addressValues(): AddressMap {
        return addresses().values
    }

    fun currentPackageAddresses(): AddressMap {
        val addresses = this.currentPackage.addresses()
        val map = mutableAddressMap()
        map.putAll(addresses.values)
        map.putAll(addresses.placeholdersAsValues())
        return map
    }

    fun getNamedAddress(name: String): Address.Named? {
        val addressVal = addressValues()[name] ?: return null
        return Address.Named(name, addressVal.value)
    }

    fun searchScope(): GlobalSearchScope {
        var searchScope = GlobalSearchScope.EMPTY_SCOPE
        for (folder in sourceFolders()) {
            val dirScope = GlobalSearchScopes.directoryScope(project, folder, true)
            searchScope = searchScope.uniteWith(dirScope)
        }
        return searchScope
    }

    fun processMoveFiles(processFile: (MoveFile) -> Boolean) {
        val folders = sourceFolders()
        var stopped = false
        for (folder in folders) {
            if (stopped) break
            folder.iterateMoveVirtualFiles {
                val moveFile = it.toMoveFile(project) ?: return@iterateMoveVirtualFiles true
                val continueForward = processFile(moveFile)
                stopped = !continueForward
                continueForward
            }
        }
    }

    companion object {
        fun forTests(project: Project): MoveProject {
            checkUnitTestMode()
            val contentRoot = project.contentRoots.first()
            val moveToml = MoveToml(project)
            val movePackage = MovePackage(project, contentRoot, moveToml)
            return MoveProject(
                project,
                movePackage,
                dependencies = emptyList()
            )
        }
    }
}
