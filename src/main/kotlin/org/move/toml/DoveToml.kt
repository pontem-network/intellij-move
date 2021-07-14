package org.move.toml

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.psi.PsiManager
import org.move.openapiext.resolveAbsPath
import org.toml.lang.psi.TomlFile
import org.toml.lang.psi.TomlInlineTable
import org.toml.lang.psi.TomlTable
import org.toml.lang.psi.TomlValue
import java.nio.file.Path


data class PackageTable(
    val name: String?,
    val account_address: String?,
    val dialect: String?,
    val blockchain_api: String?,
    val dependencies: List<Path>
)

data class LayoutTable(
    val modules_dir: Path?,
    val scripts_dir: Path?,
    val tests_dir: Path?,
)


class DoveToml(
    val packageTable: PackageTable?,
    val layoutTable: LayoutTable?
) {
    companion object {
        fun parse(project: Project, projectRoot: Path): DoveToml? {
            val file = LocalFileSystem.getInstance()
                .findFileByNioFile(projectRoot.resolve("Dove.toml")) ?: return null
            val tomlFileViewProvider =
                PsiManager.getInstance(project).findViewProvider(file) ?: return null
            val tomlFile = TomlFile(tomlFileViewProvider)
            val tables = tomlFile.children.filterIsInstance<TomlTable>()

            val packageTomlTable = tables.find { it.header.key?.text == "package" }
            var packageTable: PackageTable? = null
            if (packageTomlTable != null) {
                val name = packageTomlTable.findValue("name")?.stringValue()
                val account_address = packageTomlTable.findValue("account_address")?.stringValue() ?: "0x1"
                val dialect = packageTomlTable.findValue("dialect")?.stringValue() ?: "pont"
                val blockchain_api = packageTomlTable.findValue("blockchain_api")?.stringValue()

                val depEntries = packageTomlTable.findValue("dependencies")?.arrayValue().orEmpty()
                val dependencies = parseDependencies(depEntries, projectRoot)

                packageTable = PackageTable(name, account_address, dialect, blockchain_api, dependencies)
            }

            val layoutTomlTable = tables.find { it.header.key?.text == "package" }
            var layoutTable: LayoutTable? = null
            if (layoutTomlTable != null) {
                val modulesDirValue = layoutTomlTable.findValue("modules_dir")?.stringValue() ?: "./modules"
                val modules_dir = projectRoot.resolveAbsPath(modulesDirValue)
                layoutTable = LayoutTable(modules_dir)
            }
            return DoveToml(packageTable, layoutTable)
        }

        private const val EXTERNAL_DIRECTORY_PATH = "./artifacts/.external"

        private fun parseDependencies(depEntries: List<TomlValue>, projectRoot: Path): List<Path> {
            return depEntries
                .filterIsInstance<TomlInlineTable>()
                .mapNotNull {
                    when {
                        it.hasKey("git") -> {
                            val gitDepsRoot =
                                projectRoot.resolveAbsPath(EXTERNAL_DIRECTORY_PATH)
                                    ?: return@mapNotNull null

                        }
                        else -> it.findValue("path")
//                            it.hasKey("path") -> it.findValue("path")
//                            else -> null
//                            it.findValue("git") != null -> null
//                            it.findValue("path") != null ->
//                            else -> it.findValue("path")?.stringValue()
                    }
                }
//                    .mapNotNull { it.findValue("path")?.stringValue() }
                .mapNotNull { projectRoot.resolveAbsPath(it) }
//            return dependencies
        }
    }
}
