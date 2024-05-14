package org.move.cli.runConfigurations

import com.intellij.execution.process.ProcessListener
import com.intellij.execution.process.ProcessOutput
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import org.move.openapiext.RsProcessResult
import org.move.openapiext.execute
import java.nio.file.Path

abstract class BlockchainCli(parentDisposable: Disposable?): Disposable {

    init {
        if (parentDisposable != null) {
            Disposer.register(parentDisposable, this)
        }
    }

    override fun dispose() {}

    abstract val cliLocation: Path

    abstract fun init(
        project: Project,
        rootDirectory: VirtualFile,
        packageName: String,
    ): RsProcessResult<VirtualFile>

    abstract fun fetchPackageDependencies(
        project: Project,
        projectDir: Path,
        skipLatest: Boolean,
        processListener: ProcessListener
    ): RsProcessResult<Unit>

    protected fun executeCommandLine(
        commandLine: CliCommandLineArgs,
        listener: ProcessListener? = null
    ): RsProcessResult<ProcessOutput> {
        return commandLine
            .toGeneralCommandLine(this.cliLocation)
            .execute(this, stdIn = null, listener = listener)
    }
}