package org.move.cli.runConfigurations

import com.intellij.execution.process.ProcessListener
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.move.openapiext.RsProcessResult
import java.nio.file.Path

abstract class BlockchainCli {

    abstract val cliLocation: Path

    abstract fun init(
        project: Project,
        parentDisposable: Disposable,
        rootDirectory: VirtualFile,
        packageName: String,
    ): RsProcessResult<VirtualFile>

    abstract fun fetchPackageDependencies(
        project: Project,
        projectDir: Path,
        skipLatest: Boolean,
        owner: Disposable,
        processListener: ProcessListener
    ): RsProcessResult<Unit>
}