package org.move.cli.manifest

import com.intellij.util.io.readText
import org.yaml.snakeyaml.Yaml
import java.nio.file.Path

data class AptosConfigYaml(
    val configYamlPath: Path,
    val profiles: Set<String>
) {
    fun runProfiles(): List<String> = profiles.toList()

    companion object {
        fun fromPath(configYamlPath: Path): AptosConfigYaml? {
            val yaml = Yaml().load<Map<String, Any>>(configYamlPath.readText())

            @Suppress("UNCHECKED_CAST")
            val profiles = (yaml["profiles"] as? Map<*, *>)?.keys as? Set<String> ?: return null
            return AptosConfigYaml(configYamlPath, profiles)
        }
    }
}
