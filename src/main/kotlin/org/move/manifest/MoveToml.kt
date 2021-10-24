package org.move.manifest

import com.intellij.openapi.project.Project
import org.move.openapiext.*
import org.toml.lang.psi.TomlFile
import org.toml.lang.psi.TomlInlineTable
import java.nio.file.Path

typealias AddressesMap = Map<String, String>
typealias DependenciesMap = Map<String, Dependency>

data class Dependency(
    val local: Path
)

data class MoveTomlPackageTable(
    val name: String?,
    val version: String?,
    val authors: List<String>,
    val license: String?
)

class MoveToml(
    val packageTable: MoveTomlPackageTable?,
    val addresses: AddressesMap,
    val dev_addresses: AddressesMap,
    val dependencies: DependenciesMap,
    val dev_dependencies: DependenciesMap,
) {
    companion object {
        fun parse(project: Project, projectRoot: Path): MoveToml? {
            val tomlFile = parseToml(project, projectRoot.resolve("Move.toml")) ?: return null

            val packageTomlTable = tomlFile.getTable("package");
            var packageTable: MoveTomlPackageTable? = null
            if (packageTomlTable != null) {
                val name = packageTomlTable.findValue("name")?.stringValue()
                val version = packageTomlTable.findValue("version")?.stringValue()
                val authors =
                    packageTomlTable.findValue("authors")?.arrayValue().orEmpty().map { it.stringValue()!! }
                val license = packageTomlTable.findValue("license")?.stringValue()
                packageTable = MoveTomlPackageTable(name, version, authors, license)
            }

            val addresses = parseAddresses("addresses", tomlFile)
            val dev_addresses = parseAddresses("dev_addresses", tomlFile)

            val dependencies = parseDependencies("dependencies", tomlFile, projectRoot)
            val dev_dependencies = parseDependencies("dev_dependencies", tomlFile, projectRoot)

            return MoveToml(packageTable, addresses, dev_addresses, dependencies, dev_dependencies)
        }

        private fun parseAddresses(tableKey: String, tomlFile: TomlFile): AddressesMap {
            val addresses = mutableMapOf<String, String>()
            val tomlAddresses = tomlFile.getTable(tableKey)?.namedEntries().orEmpty()
            for ((addressName, tomlValue) in tomlAddresses) {
                addresses[addressName] = tomlValue?.stringValue() ?: continue
            }
            return addresses
        }

        private fun parseDependencies(
            tableKey: String,
            tomlFile: TomlFile,
            projectRoot: Path
        ): DependenciesMap {
            val dependencies = mutableMapOf<String, Dependency>()
            val tomlDeps = tomlFile.getTable(tableKey)?.namedEntries().orEmpty()
            for ((depName, tomlValue) in tomlDeps) {
                val depTable = (tomlValue as? TomlInlineTable) ?: continue
                val localPathValue = depTable.findValue("local")?.stringValue() ?: continue
                val localPath =
                    projectRoot.resolve(localPathValue).toAbsolutePath().normalize()
                dependencies[depName] = Dependency(localPath)
            }
            return dependencies
        }
    }
}
