package org.move.manifest

import com.intellij.openapi.project.Project
import org.move.openapiext.*
import org.toml.lang.psi.TomlInlineTable
import org.toml.lang.psi.TomlTable
import java.nio.file.Path
import java.nio.file.Paths

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
    val addresses: Map<String, String>,
    val dependencies: Map<String, Dependency>,
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

            val addresses = mutableMapOf<String, String>()
            val tomlAddresses = tomlFile.getTable("addresses")?.namedEntries().orEmpty()
            for ((addressName, tomlValue) in tomlAddresses) {
                addresses[addressName] = tomlValue?.stringValue() ?: continue
            }

            val dependencies = mutableMapOf<String, Dependency>()
            val tomlDeps = tomlFile.getTable("dependencies")?.namedEntries().orEmpty()
            for ((depName, tomlValue) in tomlDeps) {
                val depTable = (tomlValue as? TomlInlineTable) ?: continue
                val localPathValue = depTable.findValue("local")?.stringValue() ?: continue
                val localPath =
                    projectRoot.resolve(localPathValue).toAbsolutePath().normalize()
                dependencies[depName] = Dependency(localPath)
            }

            return MoveToml(packageTable, addresses, dependencies)
        }
    }
}
