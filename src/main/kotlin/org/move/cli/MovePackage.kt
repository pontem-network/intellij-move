package org.move.cli

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.move.cli.manifest.AptosConfigYaml
import org.move.cli.manifest.MoveToml
import org.move.lang.core.psi.MvElement
import org.move.lang.moveProject
import org.move.lang.toNioPathOrNull
import org.move.openapiext.common.isLightTestFile
import org.move.openapiext.common.isUnitTestMode
import org.move.openapiext.pathAsPath
import org.move.openapiext.resolveExisting
import org.move.openapiext.toPsiFile
import org.toml.lang.psi.TomlFile
import java.nio.file.Path
import kotlin.io.path.relativeToOrNull

data class MovePackage(
    val project: Project,
    val contentRoot: VirtualFile,
    val packageName: String,
    val tomlMainAddresses: PackageAddresses,
    ) {
    val manifestFile: VirtualFile get() = contentRoot.findChild(Consts.MANIFEST_FILE)!!

    val manifestTomlFile: TomlFile get() = manifestFile.toPsiFile(project) as TomlFile
    val moveToml: MoveToml get() = MoveToml.fromTomlFile(this.manifestTomlFile)

//    val packageName = this.moveToml.packageName ?: ""

    val sourcesFolder: VirtualFile? get() = contentRoot.takeIf { it.isValid }?.findChild("sources")
    val testsFolder: VirtualFile? get() = contentRoot.takeIf { it.isValid }?.findChild("tests")
    val scriptsFolder: VirtualFile? get() = contentRoot.takeIf { it.isValid }?.findChild("scripts")

    val aptosConfigYaml: AptosConfigYaml?
        get() {
            var root: VirtualFile? = contentRoot
            while (true) {
                if (root == null) break
                val candidatePath = root
                    .findChild(".aptos")
                    ?.takeIf { it.isDirectory }
                    ?.findChild("config.yaml")
                if (candidatePath != null) {
                    return AptosConfigYaml.fromPath(candidatePath.pathAsPath)
                }
                root = root.parent
            }
            return null
        }

    fun moveFolders(): List<VirtualFile> = listOfNotNull(sourcesFolder, testsFolder, scriptsFolder)

    fun layoutPaths(): List<Path> {
        val rootPath = contentRoot.takeIf { it.isValid }?.toNioPathOrNull() ?: return emptyList()
        val names = listOf(
            *MvProjectLayout.sourcesDirs,
            MvProjectLayout.testsDir,
            MvProjectLayout.buildDir
        )
        return names.mapNotNull { rootPath.resolveExisting(it) }
    }

    fun addresses(): PackageAddresses {
//        val tomlMainAddresses = tomlMainAddresses
//        val tomlDevAddresses = moveToml.declaredAddresses()

        val addresses = mutableAddressMap()
        addresses.putAll(tomlMainAddresses.values)
        // add placeholders defined in this package as address values
        addresses.putAll(tomlMainAddresses.placeholdersAsValues())
        // devs on top
//        addresses.putAll(tomlDevAddresses.values)

        return PackageAddresses(addresses, tomlMainAddresses.placeholders)
    }

    override fun hashCode(): Int = this.contentRoot.hashCode()

    override fun equals(other: Any?): Boolean {
        if (other !is MovePackage) return false
        if (this === other) return true
        return this.contentRoot == other.contentRoot
    }

    companion object {
        fun fromMoveToml(moveToml: MoveToml): MovePackage {
            val contentRoot = moveToml.tomlFile.virtualFile.parent
            return MovePackage(moveToml.project, contentRoot,
                               packageName = moveToml.packageName ?: "",
                               tomlMainAddresses = moveToml.declaredAddresses())
        }
    }
}

val MvElement.containingMovePackage: MovePackage?
    get() {
        val elementFile = this.containingFile.virtualFile
        if (isUnitTestMode && elementFile.isLightTestFile) {
            // temp file for light unit tests
            return project.testMoveProject.currentPackage
        }
        val elementPath = this.containingFile?.toNioPathOrNull() ?: return null
        val allPackages = this.moveProject?.movePackages().orEmpty()
        return allPackages.find {
            val folderPaths = it.moveFolders().mapNotNull { it.toNioPathOrNull() }
            for (folderPath in folderPaths) {
                if (elementPath.relativeToOrNull(folderPath) != null) {
                    return it
                }
            }
            false
        }
    }
