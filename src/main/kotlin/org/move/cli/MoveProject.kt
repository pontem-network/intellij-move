package org.move.cli

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.Service.Level.PROJECT
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsContexts.Tooltip
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.GlobalSearchScopes
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import org.move.cli.manifest.MoveToml
import org.move.cli.tests.testAddressesService
import org.move.lang.MoveFile
import org.move.lang.core.types.Address.Named
import org.move.lang.core.types.NumericAddress
import org.move.lang.toMoveFile
import org.move.lang.toNioPathOrNull
import org.move.openapiext.common.checkUnitTestMode
import org.move.openapiext.common.isUnitTestMode
import org.move.openapiext.contentRoots
import org.move.stdext.chain
import org.move.stdext.iterateMoveVirtualFiles
import org.move.stdext.wrapWithList
import org.toml.lang.TomlLanguage
import org.toml.lang.psi.TomlFile
import java.nio.file.Path

data class MovePackageWithAddrSubst(val package_: MovePackage, val addrSubst: RawAddressMap)

data class MoveProject(
    val project: Project,
    val currentPackage: MovePackage,
    val dependencies: List<MovePackageWithAddrSubst>,
    // updates
    val fetchDepsStatus: UpdateStatus = UpdateStatus.NeedsUpdate,
): UserDataHolderBase() {

    val contentRoot: VirtualFile get() = this.currentPackage.contentRoot
    val contentRootPath: Path? get() = this.currentPackage.contentRoot.toNioPathOrNull()

    fun movePackages(): Sequence<MovePackage> = currentPackage.wrapWithList().chain(depPackages())
    fun depPackages(): List<MovePackage> = dependencies.map { it.package_ }.reversed()

    fun allAccessibleMoveFolders(): List<VirtualFile> {
        val folders = currentPackage.moveFolders().toMutableList()

        val depFolders = dependencies.asReversed().flatMap { it.package_.moveFolders() }
        folders.addAll(depFolders)

        return folders
    }

    fun namedAddresses(): PackageAddresses {
        return CachedValuesManager.getManager(this.project).getCachedValue(this) {
            val packageName = this.currentPackage.packageName

            val cumulativeAddresses = PackageAddresses(mutableAddressMap(), placeholderMap())
            for ((depPackage, addrSubst) in this.dependencies) {
                cumulativeAddresses.extendWith(depPackage.addresses())
                cumulativeAddresses.applySubstitution(addrSubst, packageName)
            }
            cumulativeAddresses.extendWith(this.currentPackage.addresses())

            CachedValueProvider.Result.create(
                cumulativeAddresses,
                PsiModificationTracker.MODIFICATION_COUNT
            )
        }
    }

    fun numericTomlAddresses(): AddressMap {
        return namedAddresses().values
    }

    fun currentPackageAddresses(): AddressMap {
        val addresses = this.currentPackage.addresses()
        val map = mutableAddressMap()
        map.putAll(addresses.values)
        map.putAll(addresses.placeholdersAsValues())
        return map
    }

    fun getNamedAddress(name: String): Named? {
        val declaredNamedValue = getAddressValueByName(name)
        if (declaredNamedValue != null) {
            return Named(name)
        }
        return null
    }

    fun getAddressNamesForValue(numericAddress: NumericAddress): List<String> {
        val moveProject = this
        return buildList {
            for ((name, tomlAddress) in moveProject.numericTomlAddresses()) {
                if (tomlAddress.numericAddress == numericAddress) {
                    add(name)
                }
            }
            if (isUnitTestMode) {
                val testAddresses =
                    moveProject.project.testAddressesService.getNamedAddressesForValue(moveProject, numericAddress)
                addAll(testAddresses)
            }
        }
    }

    fun getNumericAddressByName(name: String): NumericAddress? {
        val numericValue = this.getAddressValueByName(name)
        return numericValue
//            ?.takeIf { it != "_" }
            ?.let {
                NumericAddress(it)
            }
    }

    fun getAddressValueByName(name: String): String? {
        val numericTomlAddress = numericTomlAddresses()[name]
        if (numericTomlAddress == null) {
            if (isUnitTestMode) {
                return project.testAddressesService.getNamedAddress(this, name)
            }
        }
        return numericTomlAddress?.value
    }

    fun searchScope(): GlobalSearchScope {
        var searchScope = GlobalSearchScope.EMPTY_SCOPE
        for (folder in allAccessibleMoveFolders()) {
            val dirScope = GlobalSearchScopes.directoryScope(project, folder, true)
            searchScope = searchScope.uniteWith(dirScope)
        }
        if (isUnitTestMode && searchScope == GlobalSearchScope.EMPTY_SCOPE) {
            // add current file to the search scope for the tests
            val currentFile =
                FileEditorManager.getInstance(project).selectedTextEditor?.virtualFile
            if (currentFile != null) {
                searchScope = searchScope.uniteWith(GlobalSearchScope.fileScope(project, currentFile))
            }
        }
        return searchScope
    }

    fun processMoveFiles(processFile: (MoveFile) -> Boolean) {
        val folders = allAccessibleMoveFolders()
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

    sealed class UpdateStatus(private val priority: Int) {
        //        object UpToDate : UpdateStatus(0)
        object NeedsUpdate: UpdateStatus(1)
        class UpdateFailed(@Tooltip val reason: String): UpdateStatus(2) {
            override fun toString(): String = reason
        }

//        fun merge(status: UpdateStatus): UpdateStatus = if (priority >= status.priority) this else status
    }

    val mergedStatus: UpdateStatus get() = fetchDepsStatus
//    val mergedStatus: UpdateStatus get() = fetchDepsStatus.merge(stdlibStatus)

    override fun toString(): String {
        return "MoveProject(" +
                "root=${this.contentRoot.path}, " +
                "deps=${this.depPackages().map { it.contentRoot.path }})"
    }

    companion object {
        fun packageForTests(project: Project): MovePackage {
            checkUnitTestMode()
            val contentRoot = project.contentRoots.first()
            val tomlFile =
                PsiFileFactory.getInstance(project)
                    .createFileFromText(
                        TomlLanguage,
                        """
                     [package]
                     name = "DummyPackage"
                """
                    ) as TomlFile

            val moveToml = MoveToml(project, tomlFile)
            val movePackage = MovePackage(
                project, contentRoot,
                packageName = "DummyPackage",
                tomlMainAddresses = moveToml.declaredAddresses()
            )
            return movePackage
        }

        fun forTests(project: Project): MoveProject {
            return MoveProject(
                project,
                packageForTests(project),
                dependencies = emptyList()
            )
        }
    }
}

@Service(PROJECT)
class MoveProjectTestService(val project: Project) {
    val testMoveProject: Lazy<MoveProject> get() = lazy { MoveProject.forTests(project) }
}

val Project.testMoveProject get() = this.service<MoveProjectTestService>().testMoveProject.value