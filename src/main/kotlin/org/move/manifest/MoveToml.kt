package org.move.manifest

import com.intellij.openapi.project.Project
import org.move.openapiext.arrayValue
import org.move.openapiext.findValue
import org.move.openapiext.parseToml
import org.move.openapiext.stringValue
import org.toml.lang.psi.TomlTable
import java.nio.file.Path

data class MoveTomlPackageTable(
    val name: String?,
    val version: String?,
    val authors: List<String>,
    val license: String?
)

class MoveToml(
    val packageTable: MoveTomlPackageTable?,
) {
    companion object {
        fun parse(project: Project, projectRoot: Path): MoveToml? {
            val tomlFile = parseToml(project, projectRoot.resolve("Move.toml")) ?: return null
            val tables = tomlFile.children.filterIsInstance<TomlTable>()

            val packageTomlTable = tables.find { it.header.key?.text == "package" }
            var packageTable: MoveTomlPackageTable? = null
            if (packageTomlTable != null) {
                val name = packageTomlTable.findValue("name")?.stringValue()
                val version = packageTomlTable.findValue("version")?.stringValue()
                val authors =
                    packageTomlTable.findValue("authors")?.arrayValue().orEmpty().map { it.stringValue()!! }
                val license = packageTomlTable.findValue("license")?.stringValue()
                packageTable = MoveTomlPackageTable(name, version, authors, license)
            }
            return MoveToml(packageTable)
        }
    }
}
