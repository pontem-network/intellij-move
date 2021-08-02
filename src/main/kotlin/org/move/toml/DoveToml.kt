package org.move.toml

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.psi.PsiManager
import org.move.openapiext.resolveAbsPath
import org.toml.lang.psi.*
import java.nio.file.Path


fun TomlFile.getRootKey(key: String): TomlValue? {
    val keyValue = this.children.filterIsInstance<TomlKeyValue>().find { it.key.text == key }
    return keyValue?.value
}


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
        private fun parseToml(project: Project, path: Path): TomlFile? {
            val file = LocalFileSystem.getInstance().findFileByNioFile(path) ?: return null
            val tomlFileViewProvider =
                PsiManager.getInstance(project).findViewProvider(file) ?: return null
            return TomlFile(tomlFileViewProvider)
        }

        fun parse(project: Project, projectRoot: Path): DoveToml? {
            val tomlFile = parseToml(project, projectRoot.resolve("Dove.toml")) ?: return null
            val tables = tomlFile.children.filterIsInstance<TomlTable>()

            val packageTomlTable = tables.find { it.header.key?.text == "package" }
            var packageTable: PackageTable? = null
            if (packageTomlTable != null) {
                val name = packageTomlTable.findValue("name")?.stringValue()
                val account_address = packageTomlTable.findValue("account_address")?.stringValue() ?: "0x1"
                val dialect = packageTomlTable.findValue("dialect")?.stringValue() ?: "pont"
                val blockchain_api = packageTomlTable.findValue("blockchain_api")?.stringValue()

                val depEntries = packageTomlTable.findValue("dependencies")?.arrayValue().orEmpty()
                val dependencies = parseDependencies(project, depEntries, projectRoot)

                packageTable = PackageTable(name, account_address, dialect, blockchain_api, dependencies)
            }

            val layoutTomlTable = tables.find { it.header.key?.text == "package" }
            var layoutTable: LayoutTable? = null
            if (layoutTomlTable != null) {
                fun findDir(key: String, default: String): Path? {
                    val dirValue = layoutTomlTable.findValue(key)?.stringValue() ?: default
                    return projectRoot.resolveAbsPath(dirValue)
                }
                layoutTable = LayoutTable(
                    findDir("modules_dir", "./modules"),
                    findDir("scripts_dir", "./scripts"),
                    findDir("tests_dir", "./tests")
                )
            }
            return DoveToml(packageTable, layoutTable)
        }

        private const val ARTIFACTS_DIRECTORY_PATH = "./artifacts"
        private const val DOVE_INDEX_FILE = ".DoveIndex.toml"

        private fun parseDependencies(
            project: Project,
            depEntries: List<TomlValue>,
            projectRoot: Path
        ): List<Path> {
            val deps = mutableListOf<Path>()

            val depInlineTables = depEntries.filterIsInstance<TomlInlineTable>()
            if (depInlineTables.find { it.hasKey("git") } != null) {
                // read .DoveIndex.toml file for paths to git dependencies
                val artifactsDir = projectRoot.resolveAbsPath(ARTIFACTS_DIRECTORY_PATH)
                if (artifactsDir != null) {
                    val doveIndexPath = artifactsDir.resolveAbsPath(DOVE_INDEX_FILE)
                    val doveIndexTomlFile = doveIndexPath?.let { parseToml(project, it) }
                    if (doveIndexTomlFile != null) {
                        val values =
                            parseDepsFromDoveIndex(doveIndexTomlFile)
                        deps.addAll(values.mapNotNull { artifactsDir.resolveAbsPath(it) })
                    }
                }
            }

            for (table in depInlineTables) {
                val path = table.findValue("path")?.stringValue() ?: continue
                val absPath = projectRoot.resolveAbsPath(path) ?: continue
                deps.add(absPath)
            }
            return deps
        }

        private fun parseDepsFromDoveIndex(doveIndexTomlFile: TomlFile): List<String> {
            return doveIndexTomlFile.getRootKey("deps_roots")
                ?.arrayValue().orEmpty()
                .mapNotNull { it.stringValue() }
        }
    }
}
