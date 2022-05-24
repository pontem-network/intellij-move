package org.move.cli.project

import com.intellij.util.io.readText
import org.move.cli.AddressMap
import org.move.cli.AddressVal
import org.move.lang.core.types.shortenYamlAddress
import org.yaml.snakeyaml.Yaml
import java.nio.file.Path

data class BuildInfoYaml(
    val yamlFilePath: Path,
    val compiledPackageInfo: CompiledPackageInfo,
    val dependencies: List<String>,
) {
    fun addresses(): AddressMap {
        val packageName = this.compiledPackageInfo.package_name ?: ""
        val addresses = this
            .compiledPackageInfo.address_alias_instantiation
            .mapValues {
                val shortAddress = it.value.shortenYamlAddress()
                AddressVal(shortAddress, null, null, packageName)
            }
            .toMutableMap()
        return addresses
    }

    data class CompiledPackageInfo(
        val package_name: String?,
        val address_alias_instantiation: Map<String, String>
    )

    companion object {
        fun fromPath(yamlFilePath: Path): BuildInfoYaml? {
            val yaml = Yaml().load<Map<String, Any>>(yamlFilePath.readText())

            val packageInfoMap = yaml["compiled_package_info"] as? Map<*, *> ?: return null

            @Suppress("UNCHECKED_CAST")
            val dependencies = yaml["dependencies"] as? List<String> ?: return null

            val packageName = packageInfoMap["package_name"] as? String

            @Suppress("UNCHECKED_CAST")
            val addressAliasInstantiation =
                (packageInfoMap["address_alias_instantiation"] as? Map<String, String>).orEmpty()

            val packageInfo = CompiledPackageInfo(packageName, addressAliasInstantiation)
            return BuildInfoYaml(
                yamlFilePath,
                packageInfo,
                dependencies
            )
        }
    }
}
