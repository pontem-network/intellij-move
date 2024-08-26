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
import org.move.cli.manifest.AptosConfigYaml
import org.move.cli.manifest.MoveToml
import org.move.cli.tests.NamedAddressFromTestAnnotationService
import org.move.lang.MoveFile
import org.move.lang.core.psi.MvModule
import org.move.lang.core.types.Address
import org.move.lang.core.types.AddressLit
import org.move.lang.index.MvNamedElementIndex
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

data class MoveProject(
    val project: Project,
    val currentPackage: MovePackage,
    val dependencies: List<Pair<MovePackage, RawAddressMap>>,
    // updates
    val fetchDepsStatus: UpdateStatus = UpdateStatus.NeedsUpdate,
): UserDataHolderBase() {

    val contentRoot: VirtualFile get() = this.currentPackage.contentRoot
    val contentRootPath: Path? get() = this.currentPackage.contentRoot.toNioPathOrNull()

    fun movePackages(): Sequence<MovePackage> = currentPackage.wrapWithList().chain(depPackages())
    fun depPackages(): List<MovePackage> = dependencies.map { it.first }.reversed()

    fun allAccessibleMoveFolders(): List<VirtualFile> {
        val folders = currentPackage.moveFolders().toMutableList()

        val depFolders = dependencies.asReversed().flatMap { it.first.moveFolders() }
        folders.addAll(depFolders)

        return folders
    }

    fun addresses(): PackageAddresses {
        return CachedValuesManager.getManager(this.project).getCachedValue(this) {
            val packageName = this.currentPackage.packageName

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

    fun getNamedAddressTestAware(name: String): Address.Named? {
        val declaredNamedValue = getValueOfDeclaredNamedAddress(name)
        if (declaredNamedValue != null) {
            return Address.Named(name, declaredNamedValue)
        }
        if (isUnitTestMode) {
            val namedAddressService = project.service<NamedAddressFromTestAnnotationService>()
            return namedAddressService.getNamedAddress(this, name)
        }
        return null
    }

    fun getValueOfDeclaredNamedAddress(name: String): String? = addressValues()[name]?.value

    fun getAddressNamesForValue(addressValue: String): List<String> {
        val addressLit = AddressLit(addressValue)
        val names = mutableListOf<String>()
        for ((name, value) in addresses().values.entries) {
            val canonicalValue = value.literal.canonical()
            if (canonicalValue == addressLit.canonical()) {
                names.add(name)
            }
        }
        return names
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

    fun getModulesFromIndex(name: String): Collection<MvModule> {
        return MvNamedElementIndex
            .getElementsByName(project, name, searchScope())
            .filterIsInstance<MvModule>()
    }

    val aptosConfigYaml: AptosConfigYaml? get() = this.currentPackage.aptosConfigYaml

    val profiles: Set<String> = this.aptosConfigYaml?.profiles.orEmpty()

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

        fun merge(status: UpdateStatus): UpdateStatus = if (priority >= status.priority) this else status
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
            val movePackage = MovePackage(project, contentRoot,
                                          packageName = "DummyPackage",
                                          tomlMainAddresses = moveToml.declaredAddresses())
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