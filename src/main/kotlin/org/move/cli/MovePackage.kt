package org.move.cli

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.move.cli.manifest.AptosConfigYaml
import org.move.cli.manifest.MoveToml
import org.move.lang.core.psi.MvElement
import org.move.lang.moveProject
import org.move.lang.toNioPathOrNull
import org.move.openapiext.pathAsPath
import org.move.openapiext.resolveExisting
import java.nio.file.Path
import kotlin.io.path.relativeToOrNull

data class MovePackage(
    val project: Project,
    val contentRoot: VirtualFile,
    val moveToml: MoveToml,
) {
    val packageName = this.moveToml.packageName ?: ""

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
        val tomlMainAddresses = moveToml.declaredAddresses()
        val tomlDevAddresses = moveToml.declaredAddresses()

        val addresses = mutableAddressMap()
        addresses.putAll(tomlMainAddresses.values)
        // add placeholders defined in this package as address values
        addresses.putAll(tomlMainAddresses.placeholdersAsValues())
        // devs on top
        addresses.putAll(tomlDevAddresses.values)

        return PackageAddresses(addresses, tomlMainAddresses.placeholders)
    }

    override fun hashCode(): Int {
        return this.moveToml.tomlFile.toNioPathOrNull()?.hashCode() ?: this.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (other !is MovePackage) return false
        val leftPath = this.moveToml.tomlFile.toNioPathOrNull() ?: return false
        val rightPath = other.moveToml.tomlFile.toNioPathOrNull() ?: return false
        return leftPath == rightPath
    }

    companion object {
        fun fromMoveToml(moveToml: MoveToml): MovePackage {
            val contentRoot = moveToml.tomlFile.virtualFile.parent
            return MovePackage(moveToml.project, contentRoot, moveToml)
        }
    }
}

val MvElement.containingMovePackage: MovePackage?
    get() {
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
