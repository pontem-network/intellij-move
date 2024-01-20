package org.move.cli.manifest

import com.intellij.util.io.readText
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.error.YAMLException
import java.nio.file.Path

data class AptosConfigYaml(
    val configYamlPath: Path,
    val profiles: Set<String>
) {
    companion object {
        fun fromPath(configYamlPath: Path): AptosConfigYaml? {
            val yaml =
                try {
                    Yaml().load<Map<String, Any>>(configYamlPath.readText())
                } catch (e: YAMLException) {
                    // TODO: error notification?
                    return null
                }
            @Suppress("UNCHECKED_CAST")
            val profiles = (yaml["profiles"] as? Map<*, *>)?.keys as? Set<String> ?: return null
            return AptosConfigYaml(configYamlPath, profiles)
        }
    }
}
