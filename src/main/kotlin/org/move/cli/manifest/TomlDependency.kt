package org.move.cli.manifest

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
            val homeDir = System.getProperty("user.home")
            val sanitizedRepoName = repo.replace(Regex("[/:.@]"), "_")
            val revName = rev.replace('/', '_')
            return Paths.get(homeDir, ".move", "${sanitizedRepoName}_$revName")
        }
    }
}
