package org.move.cli

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.move.cli.manifest.MoveToml
import org.move.lang.toNioPathOrNull
import org.move.openapiext.checkReadAccessAllowed
import org.move.openapiext.resolveExisting
import java.nio.file.Path

data class MovePackage(
    private val project: Project,
    val contentRoot: VirtualFile,
    val moveToml: MoveToml,
) {
    val packageName = this.moveToml.packageName ?: ""

    val sourcesFolder: VirtualFile? get() = contentRoot.findChild("sources")
    val testsFolder: VirtualFile? get() = contentRoot.findChild("tests")

    fun layoutPaths(): List<Path> {
        val rootPath = contentRoot.toNioPathOrNull() ?: return emptyList()
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

    companion object {
        fun fromMoveToml(moveToml: MoveToml): MovePackage? {
            checkReadAccessAllowed()
            val contentRoot = moveToml.tomlFile?.parent?.virtualFile ?: return null
            return MovePackage(moveToml.project, contentRoot, moveToml)
        }
    }
}
