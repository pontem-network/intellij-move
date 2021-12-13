package org.move.cli

import com.intellij.openapi.project.Project
import org.move.openapiext.*
import org.toml.lang.psi.TomlFile
import org.toml.lang.psi.TomlInlineTable
import org.toml.lang.psi.TomlKeyValue
import java.nio.file.Path
import java.util.*

typealias RawAddressVal = Pair<String, TomlKeyValue>

data class AddressVal(
    val value: String,
    val keyValue: TomlKeyValue?,
    val placeholderKeyValue: TomlKeyValue?)

typealias RawAddressMap = MutableMap<String, RawAddressVal>
typealias AddressMap = MutableMap<String, AddressVal>
typealias PlaceholderMap = MutableMap<String, TomlKeyValue>

typealias DependenciesMap = SortedMap<String, Dependency>

fun mutableRawAddressMap(): RawAddressMap = mutableMapOf()
fun mutableAddressMap(): AddressMap = mutableMapOf()
fun placeholderMap(): PlaceholderMap = mutableMapOf()

fun AddressMap.copyMap(): AddressMap = this.toMutableMap()

data class Dependency(
    val absoluteLocalPath: Path,
    val subst: RawAddressMap,
)

data class MoveTomlPackageTable(
    val name: String?,
    val version: String?,
    val authors: List<String>,
    val license: String?
)

class MoveToml(
    val project: Project,
    val tomlFile: TomlFile? = null,
    val packageTable: MoveTomlPackageTable? = null,
    val addresses: RawAddressMap = mutableRawAddressMap(),
    val dev_addresses: RawAddressMap = mutableRawAddressMap(),
    val dependencies: DependenciesMap = sortedMapOf(),
    val dev_dependencies: DependenciesMap = sortedMapOf(),
) {
    companion object {
        fun fromTomlFile(tomlFile: TomlFile, projectRoot: Path): MoveToml {
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
                tomlFile,
                packageTable,
                addresses,
                dev_addresses,
                dependencies,
                dev_dependencies
            )
        }

        private fun parseAddresses(tableKey: String, tomlFile: TomlFile): RawAddressMap {
            val addresses = mutableMapOf<String, RawAddressVal>()
            val tomlAddresses = tomlFile.getTable(tableKey)?.namedEntries().orEmpty()
            for ((addressName, tomlValue) in tomlAddresses) {
                val value = tomlValue?.stringValue() ?: continue
                addresses[addressName] = Pair(value, tomlValue.keyValue)
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
            for ((depName, tomlTableValue) in tomlDeps) {
                val depTable = (tomlTableValue as? TomlInlineTable) ?: continue
                val localPathValue = depTable.findValue("local")?.stringValue() ?: continue
                val localPath =
                    projectRoot.resolve(localPathValue).toAbsolutePath().normalize()
                val substEntries =
                    depTable.findValue("addr_subst")?.inlineTableValue()?.namedEntries().orEmpty()
                val subst = mutableRawAddressMap()
                for ((name, tomlValue) in substEntries) {
                    if (tomlValue == null) continue
                    val value = tomlValue.stringValue() ?: continue
                    val keyValue = tomlValue.keyValue
                    subst[name] = Pair(value, keyValue)
                }
                dependencies[depName] = Dependency(localPath, subst)
            }
            return dependencies
        }
    }
}
