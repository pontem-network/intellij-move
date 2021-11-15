package org.move.cli

import com.intellij.openapi.project.Project
import org.move.lang.toNioPathOrNull
import org.move.openapiext.*
import org.toml.lang.psi.TomlFile
import org.toml.lang.psi.TomlInlineTable
import java.nio.file.Path
import java.util.*

typealias DependenciesMap = SortedMap<String, Dependency>

data class Dependency(
    val absoluteLocalPath: Path,
    val addrSubst: Map<String, String>,
)

data class MoveTomlPackageTable(
    val name: String?,
    val version: String?,
    val authors: List<String>,
    val license: String?
)

class MoveToml(
    val project: Project,
    // TODO: change into VirtualFile and move to MoveProject
//    val root: Path,
    val tomlFile: TomlFile?,
    val packageTable: MoveTomlPackageTable?,
    val addresses: AddressesMap,
    val dev_addresses: AddressesMap,
    val dependencies: DependenciesMap,
    val dev_dependencies: DependenciesMap,
) {
    companion object {
        fun fromTomlFile(tomlFile: TomlFile, projectRoot: Path): MoveToml {
//            val tomlFileRoot = tomlFile.toNioPathOrNull()?.parent ?: return null

            val packageTomlTable = tomlFile.getTable("package")
            var packageTable: MoveTomlPackageTable? = null
            if (packageTomlTable != null) {
                val name = packageTomlTable.findValue("name")?.stringValue()
                val version = packageTomlTable.findValue("version")?.stringValue()
                val authors =
                    packageTomlTable.findValue("authors")?.arrayValue()
                        .orEmpty()
                        .mapNotNull { it.stringValue() }
                val license = packageTomlTable.findValue("license")?.stringValue()
                packageTable = MoveTomlPackageTable(name, version, authors, license)
            }

            val addresses = parseAddresses("addresses", tomlFile)
            val dev_addresses = parseAddresses("dev_addresses", tomlFile)

            val dependencies = parseDependencies("dependencies", tomlFile, projectRoot)
            val dev_dependencies = parseDependencies("dev_dependencies", tomlFile, projectRoot)

            return MoveToml(
                tomlFile.project,
//                tomlFileRoot,
                tomlFile,
                packageTable,
                addresses,
                dev_addresses,
                dependencies,
                dev_dependencies
            )
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
            val dependencies = sortedMapOf<String, Dependency>()
            val tomlDeps = tomlFile.getTable(tableKey)?.namedEntries().orEmpty()
            for ((depName, tomlValue) in tomlDeps) {
                val depTable = (tomlValue as? TomlInlineTable) ?: continue
                val localPathValue = depTable.findValue("local")?.stringValue() ?: continue
                val localPath =
                    projectRoot.resolve(localPathValue).toAbsolutePath().normalize()
                val subst = depTable.findValue("addr_subst")
                    ?.inlineTableValue()
                    ?.entries.orEmpty()
                    .map { Pair(it.singleSegmentOrNull()?.name?.trim('"'), it.value?.stringValue()) }
                    .filter { it.first != null && it.second != null }
                    .map { Pair(it.first!!, it.second!!) }
                    .toMap()
                dependencies[depName] = Dependency(localPath, subst)
            }
            return dependencies
        }
    }
}
