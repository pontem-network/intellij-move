package org.move.cli.manifest

import com.intellij.openapi.project.Project
import org.move.cli.*
import org.move.lang.toNioPathOrNull
import org.move.openapiext.*
import org.move.stdext.chain
import org.toml.lang.psi.TomlFile
import java.nio.file.Path

data class DepWithAddrSubst(val dep: TomlDependency, val addrSubst: RawAddressMap)

class MoveToml(
    val project: Project,
    val tomlFile: TomlFile,
    val packageTable: MoveTomlPackageTable? = null,

    val addresses: RawAddressMap = mutableRawAddressMap(),
    val dev_addresses: RawAddressMap = mutableRawAddressMap(),

    val deps: List<DepWithAddrSubst> = emptyList(),
    val dev_deps: List<DepWithAddrSubst> = emptyList()
) {
    val packageName: String? get() = packageTable?.name

    fun declaredAddresses(): PackageAddresses {
        val packageName = this.packageName ?: ""
        val raws = this.addresses

        val values = mutableAddressMap()
        val placeholders = placeholderMap()
        for ((addressName, addressVal) in raws.entries) {
            val (value, tomlKeyValue) = addressVal
            if (addressVal.first == MvConstants.ADDR_PLACEHOLDER) {
                placeholders[addressName] = PlaceholderVal(tomlKeyValue, packageName)
            } else {
                values[addressName] = TomlAddress(value, tomlKeyValue, null, packageName)
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
        fun fromTomlFile(tomlFile: TomlFile): MoveToml {
            // needs read access for Toml
            checkReadAccessAllowed()

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

            val contentRoot = tomlFile.parent?.virtualFile?.toNioPathOrNull()
            val deps = contentRoot?.let { parseDependencies("dependencies", tomlFile, it) }.orEmpty()
            val devDeps = contentRoot?.let { parseDependencies("dev-dependencies", tomlFile, it) }.orEmpty()

            return MoveToml(
                tomlFile.project,
                tomlFile,
                packageTable,
                addresses,
                devAddresses,
                deps,
                devDeps
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
        ): List<DepWithAddrSubst> {
            val tomlInlineTableDeps = tomlFile.getTable(tableKey)
                ?.namedEntries().orEmpty()
                .map { Pair(it.first, it.second?.toMap().orEmpty()) }
            val tomlTableDeps = tomlFile
                .getTablesByFirstSegment(tableKey)
                .map { Pair(it.header.key?.segments?.get(1)!!.text, it.toMap()) }

            val dependencies = mutableListOf<DepWithAddrSubst>()
            for ((depName, depMap) in tomlInlineTableDeps.chain(tomlTableDeps)) {
                val depWithAddrSubst = when {
                    depMap.containsKey("local") -> parseLocalDependency(depName, depMap, projectRoot)
                    depMap.containsKey("git") -> parseGitDependency(depName, depMap)
                    else -> null
                } ?: continue
                dependencies.add(depWithAddrSubst)
            }
            return dependencies
        }

        private fun parseLocalDependency(
            depName: String,
            depTable: TomlElementMap,
            projectRoot: Path
        ): DepWithAddrSubst? {
            val localPathValue = depTable["local"]?.stringValue() ?: return null
            val normalizedLocalPath =
                projectRoot.resolve(localPathValue).toAbsolutePath().normalize()
            val subst = parseAddrSubst(depTable)
            return DepWithAddrSubst(TomlDependency.Local(depName, normalizedLocalPath), subst)
        }

        private fun parseGitDependency(
            depName: String,
            depTable: TomlElementMap,
        ): DepWithAddrSubst? {
            val repo = depTable["git"]?.stringValue() ?: return null
            val rev = depTable["rev"]?.stringValue() ?: return null
            val subdir = depTable["subdir"]?.stringValue() ?: ""
            val subst = parseAddrSubst(depTable)
            return DepWithAddrSubst(
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
