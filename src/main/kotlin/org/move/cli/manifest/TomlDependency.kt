package org.move.cli.manifest

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

sealed class TomlDependency {
    abstract val name: String

    abstract fun localPath(): Path

    data class Local(
        override val name: String,
        private val localPath: Path,
    ) : TomlDependency() {
        override fun localPath(): Path = localPath
    }

    data class Git(
        override val name: String,
        private val repo: String,
        private val rev: String,
        private val subdir: String,
    ) : TomlDependency() {

        override fun localPath(): Path {
            val home = System.getProperty("user.home")
            // TODO: add choice based on selected blockchain
            val dirNameAptos = dirNameAptos(repo, rev)
            val aptosPath = Paths.get(home, ".move", dirNameAptos, subdir)
            if (Files.exists(aptosPath)) {
                return aptosPath
            } else {
                val dirNameSui = dirNameSui(repo, rev)
                val suiPath = Paths.get(home, ".move", dirNameSui, subdir)
                return suiPath
            }
        }

        companion object {
            fun dirNameAptos(repo: String, rev: String): String {
                val sanitizedRepoName = repo.replace(Regex("[/:.@]"), "_")
                val aptosRevName = rev.replace("/", "_")
                return "${sanitizedRepoName}_$aptosRevName"
            }
            fun dirNameSui(repo: String, rev: String): String {
                val sanitizedRepoName = repo.replace(Regex("[/:.@]"), "_")
                val suiRevName = rev.replace("/", "__")
                return "${sanitizedRepoName}_$suiRevName"
            }
        }
    }
}
