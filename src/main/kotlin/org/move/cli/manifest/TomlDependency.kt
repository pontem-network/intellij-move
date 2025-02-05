package org.move.cli.manifest

import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.findDirectory
import com.intellij.util.SystemProperties
import com.intellij.util.concurrency.annotations.RequiresReadLock
import org.move.stdext.RsResult
import org.move.stdext.RsResult.Err
import org.move.stdext.RsResult.Ok
import org.move.stdext.exists
import org.move.stdext.toPath
import java.nio.file.Path

sealed class TomlDependency {
    abstract val name: String

    @RequiresReadLock
    abstract fun rootDirectory(): RsResult<VirtualFile, DependencyError>

    data class Local(
        override val name: String,
        private val localPath: Path,
    ): TomlDependency() {

        @RequiresReadLock
        override fun rootDirectory(): RsResult<VirtualFile, DependencyError> {
            val vFile = VfsUtil.findFile(localPath, false)
            if (vFile == null) {
                return Err(DependencyError("Cannot find dependency folder: $localPath"))
            }
            return Ok(vFile)
        }
    }

    data class Git(
        override val name: String,
        private val repo: String,
        val rev: String,
        private val subdir: String,
    ): TomlDependency() {

        @RequiresReadLock
        override fun rootDirectory(): RsResult<VirtualFile, DependencyError> {
            val moveHomePath = moveHome()
            if (!moveHomePath.exists()) {
                return Err(DependencyError("$moveHomePath directory does not exist"))
            }
            val depDirName = dependencyDirName(repo, rev)
            val depRoot = moveHomePath.resolve(depDirName).resolve(subdir)
            // NOTE: VFS cannot be refreshed from the read action
            val depRootFile = VfsUtil.findFile(depRoot, false)
            if (depRootFile == null) {
                return Err(DependencyError("cannot find folder: $depRoot"))
            }
            return Ok(depRootFile)
        }

        companion object {
            fun moveHome(): Path = SystemProperties.getUserHome().toPath().resolve(".move")

            fun dependencyDirName(repo: String, rev: String): String {
                val sanitizedRepoName = repo.replace(Regex("[/:.@]"), "_")
                val aptosRevName = rev.replace("/", "_")
                return "${sanitizedRepoName}_$aptosRevName"
            }
        }
    }
}

data class DependencyError(val message: String)
