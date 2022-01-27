package org.move.cli

import com.intellij.util.io.readText
import org.move.lang.core.types.shortenYamlAddress
import org.move.openapiext.resolveExisting
import org.yaml.snakeyaml.Yaml
import java.nio.file.Path

data class CompiledPackageInfo(
    val package_name: String?,
    val address_alias_instantiation: Map<String, String>
)

data class BuildInfo(
    val compiledPackageInfo: CompiledPackageInfo,
    val dependencies: List<String>
) {
    fun addresses(): AddressMap {
        val packageName = compiledPackageInfo.package_name ?: ""
        val addresses = this
            .compiledPackageInfo.address_alias_instantiation
            .mapValues {
                val shortAddress = it.value.shortenYamlAddress()
                AddressVal(shortAddress, null, null, packageName)
            }
            .toMutableMap()
        return addresses
    }

    companion object {
        fun fromRootPath(rootDirPath: Path): BuildInfo? {
            val yamlFilePath = rootDirPath.resolveExisting("BuildInfo.yaml") ?: return null
            val yamlText = yamlFilePath.readText()
            return fromText(yamlText)
        }

        fun fromText(text: String): BuildInfo? {
            val yaml = Yaml().load<Map<String, Any>>(text.reader())

            val packageInfo = yaml["compiled_package_info"] as? Map<*, *> ?: return null

            @Suppress("UNCHECKED_CAST")
            val dependencies = yaml["dependencies"] as? List<String> ?: return null

            val packageName = packageInfo["package_name"] as? String

            @Suppress("UNCHECKED_CAST")
            val addressAliasInstantiation =
                (packageInfo["address_alias_instantiation"] as? Map<String, String>).orEmpty()

            return BuildInfo(
                CompiledPackageInfo(packageName, addressAliasInstantiation), dependencies
            )
        }
    }
}
