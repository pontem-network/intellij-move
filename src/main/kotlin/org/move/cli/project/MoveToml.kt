package org.move.cli.project

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
    val dependencies: DepsSubstMap = mutableMapOf(),
    val dev_dependencies: DepsSubstMap = mutableMapOf(),
) {
    val packageName: String? get() = packageTable?.name

    fun declaredAddresses(devMode: DevMode): DeclaredAddresses {
        val packageName = this.packageName ?: ""
        val raws = when (devMode) {
            DevMode.MAIN -> this.addresses
            DevMode.DEV -> this.dev_addresses
        }

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
        return DeclaredAddresses(values, placeholders)
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

            val dependencies = parseDependencies("dependencies", tomlFile, projectRoot)
            val devDependencies = parseDependencies("dev-dependencies", tomlFile, projectRoot)

            return MoveToml(
                tomlFile.project,
                tomlFile,
                packageTable,
                addresses,
                devAddresses,
                dependencies,
                devDependencies
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
        ): DepsSubstMap {
            val tomlInlineTableDeps = tomlFile.getTable(tableKey)
                ?.namedEntries().orEmpty()
                .map { Pair(it.first, it.second?.toMap().orEmpty()) }
            val tomlTableDeps = tomlFile
                .getTablesByFirstSegment(tableKey)
                .map { Pair(it.header.key?.segments?.get(1)!!.text, it.toMap()) }

            val dependencies = mutableMapOf<String, Pair<Dependency, RawAddressMap>>()
            for ((depName, depMap) in tomlInlineTableDeps.chain(tomlTableDeps)) {
                val depPair = when {
                    depMap.containsKey("local") -> parseLocalDependency(depMap, projectRoot)
                    depMap.containsKey("git") -> parseGitDependency(depName, depMap, projectRoot)
                    else -> null
                } ?: continue
                dependencies[depName] = depPair
            }
            return dependencies
        }

        private fun parseLocalDependency(
            depTable: TomlElementMap,
            projectRoot: Path
        ): Pair<Dependency.Local, RawAddressMap>? {
            val localPathValue = depTable["local"]?.stringValue() ?: return null
            val localPath =
                projectRoot.resolve(localPathValue).toAbsolutePath().normalize()
            val subst = parseAddrSubst(depTable)
            return Pair(Dependency.Local(localPath), subst)
        }

        private fun parseGitDependency(
            depName: String,
            depTable: TomlElementMap,
            projectRoot: Path
        ): Pair<Dependency.Git, RawAddressMap> {
            val dirPath = projectRoot.resolve("build").resolve(depName)
            val subst = parseAddrSubst(depTable)
            return Pair(Dependency.Git(dirPath), subst)
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
