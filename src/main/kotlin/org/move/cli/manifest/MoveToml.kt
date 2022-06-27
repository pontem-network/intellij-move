package org.move.cli.manifest

import com.intellij.openapi.project.Project
import org.move.cli.*
import org.move.openapiext.*
import org.move.stdext.chain
import org.toml.lang.psi.TomlFile
import java.nio.file.Path

class MoveToml(
    val project: Project,
    val tomlFile: TomlFile? = null,
    val packageTable: MoveTomlPackageTable? = null,

    val addresses: RawAddressMap = mutableRawAddressMap(),
    val dev_addresses: RawAddressMap = mutableRawAddressMap(),

    val deps: List<Pair<TomlDependency, RawAddressMap>> = emptyList(),
//    val dev_deps: List<Pair<TomlDependency, RawAddressMap>> = emptyList()
) {
    val packageName: String? get() = packageTable?.name

    fun declaredAddresses(): PackageAddresses {
        val packageName = this.packageName ?: ""
        val raws = this.addresses

        val values = mutableAddressMap()
        val placeholders = placeholderMap()
        for ((addressName, addressVal) in raws.entries) {
            val (value, tomlKeyValue) = addressVal
            if (addressVal.first == Consts.ADDR_PLACEHOLDER) {
                placeholders[addressName] = PlaceholderVal(tomlKeyValue, packageName)
            } else {
                values[addressName] = AddressVal(value, tomlKeyValue, null, packageName)
            }
        }
        return PackageAddresses(values, placeholders)
    }

    data class MoveTomlPackageTable(
        val name: String?,
        val version: String?,
        val authors: List<String>,
        val license: String?
    )

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
            val devAddresses = parseAddresses("dev-addresses", tomlFile)

            val deps = parseDependencies("dependencies", tomlFile, projectRoot)
//            val dev_deps = parseDependencies2("dev-dependencies", tomlFile, projectRoot)
            return MoveToml(
                tomlFile.project,
                tomlFile,
                packageTable,
                addresses,
                devAddresses,
                deps,
//                dev_deps
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
        ): List<Pair<TomlDependency, RawAddressMap>> {
            val tomlInlineTableDeps = tomlFile.getTable(tableKey)
                ?.namedEntries().orEmpty()
                .map { Pair(it.first, it.second?.toMap().orEmpty()) }
            val tomlTableDeps = tomlFile
                .getTablesByFirstSegment(tableKey)
                .map { Pair(it.header.key?.segments?.get(1)!!.text, it.toMap()) }

            val dependencies = mutableListOf<Pair<TomlDependency, RawAddressMap>>()
            for ((depName, depMap) in tomlInlineTableDeps.chain(tomlTableDeps)) {
                val depPair = when {
                    depMap.containsKey("local") -> parseLocalDependency(depName, depMap, projectRoot)
                    depMap.containsKey("git") -> parseGitDependency(depName, depMap)
                    else -> null
                } ?: continue
                dependencies.add(depPair)
            }
            return dependencies
        }

        private fun parseLocalDependency(
            depName: String,
            depTable: TomlElementMap,
            projectRoot: Path
        ): Pair<TomlDependency.Local, RawAddressMap>? {
            val localPathValue = depTable["local"]?.stringValue() ?: return null
            val localPath =
                projectRoot.resolve(localPathValue).toAbsolutePath().normalize()
            val subst = parseAddrSubst(depTable)
            return Pair(TomlDependency.Local(depName, localPath), subst)
        }

        private fun parseGitDependency(
            depName: String,
            depTable: TomlElementMap,
        ): Pair<TomlDependency.Git, RawAddressMap>? {
            val repo = depTable["git"]?.stringValue() ?: return null
            val rev = depTable["rev"]?.stringValue() ?: return null
            val subdir = depTable["subdir"]?.stringValue() ?: ""
            val subst = parseAddrSubst(depTable)
            return Pair(
                TomlDependency.Git(
                    depName,
                    repo,
                    rev,
                    subdir,
                ), subst
            )
        }

        private fun parseAddrSubst(depTable: TomlElementMap): RawAddressMap {
            val substEntries =
                depTable["addr_subst"]?.inlineTableValue()?.namedEntries().orEmpty()
            val subst = mutableRawAddressMap()
            for ((name, tomlValue) in substEntries) {
                if (tomlValue == null) continue
                val value = tomlValue.stringValue() ?: continue
                val keyValue = tomlValue.keyValue
                subst[name] = Pair(value, keyValue)
            }
            return subst
        }
    }
}
