package org.move.cli.manifest

import org.move.openapiext.common.isUnitTestMode
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
            val dirName = dirName(repo, rev)
            return Paths.get(home, ".move", dirName, subdir)
//            return path(repo, rev, subdir)
//            val sanitizedRepoName = repo.replace(Regex("[/:.@]"), "_")
//            val revName = rev.replace('/', '_')
//            return Paths.get(homeDir, ".move", "${sanitizedRepoName}_$revName", subdir)
        }

        companion object {
            fun dirName(repo: String, rev: String): String {
                val sanitizedRepoName = repo.replace(Regex("[/:.@]"), "_")
                val revName = rev.replace('/', '_')
                return "${sanitizedRepoName}_$revName"
            }
        }
    }
}
