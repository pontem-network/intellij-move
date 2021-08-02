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

        private const val ARTIFACTS_DIRECTORY_PATH = "./.artifacts"
        private const val DOVE_MAN_PATH = "$ARTIFACTS_DIRECTORY_PATH/.Dove.man"

        private fun parseDependencies(
            project: Project,
            depEntries: List<TomlValue>,
            projectRoot: Path
        ): List<Path> {
            val deps = mutableListOf<Path>()

            val depInlineTables = depEntries.filterIsInstance<TomlInlineTable>()
            // TODO: add when .Doveman.toml is fixed
//            if (depInlineTables.find { it.hasKey("git") } != null) {
//                // read .Dove.man file for paths to git dependencies
//                val doveManPath = projectRoot.resolveAbsPath(DOVE_MAN_PATH)
//                val doveManTomlFile = doveManPath?.let { parseToml(project, it) }
//                if (doveManTomlFile != null) {
//                    deps.addAll(parseDependenciesFromDoveManFile(doveManTomlFile))
//                }
//            }

            for (table in depInlineTables) {
                val path = table.findValue("path")?.stringValue() ?: continue
                val absPath = projectRoot.resolveAbsPath(path) ?: continue
                deps.add(absPath)
            }
            return deps
        }

//        private fun parseDependenciesFromDoveManFile(doveManTomlFile: TomlFile): List<Path> {
//            println(doveManTomlFile.children)
//            return emptyList()
//            val doveManPath = projectRoot.resolveAbsPath(DOVE_MAN_PATH) ?: return emptyList()
//            val doveMan = DoveMan
//        }
    }
}
