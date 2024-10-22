package org.move.cli.manifest

import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.findDirectory
import com.intellij.util.concurrency.annotations.RequiresReadLock
import java.nio.file.Path

sealed class TomlDependency {
    abstract val name: String

    @RequiresReadLock
    abstract fun rootDirectory(): VirtualFile?

    data class Local(
        override val name: String,
        private val localPath: Path,
    ) : TomlDependency() {

        @RequiresReadLock
        override fun rootDirectory(): VirtualFile? = VfsUtil.findFile(localPath, true)
    }

    data class Git(
        override val name: String,
        private val repo: String,
        private val rev: String,
        private val subdir: String,
    ) : TomlDependency() {

        @RequiresReadLock
        override fun rootDirectory(): VirtualFile? {
            val userHome = VfsUtil.getUserHomeDir() ?: return null
            val sourceDirName = dirNameAptos(repo, rev)
            return userHome.findDirectory(".move/$sourceDirName/$subdir")
        }

        companion object {
            fun dirNameAptos(repo: String, rev: String): String {
                val sanitizedRepoName = repo.replace(Regex("[/:.@]"), "_")
                val aptosRevName = rev.replace("/", "_")
                return "${sanitizedRepoName}_$aptosRevName"
            }
        }
    }
}
