/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.move.cli.runConfigurations

import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.CapturingAnsiEscapesAwareProcessHandler
import com.intellij.util.io.BaseOutputReader
import org.move.stdext.RsResult

class MvCapturingProcessHandler private constructor(commandLine: GeneralCommandLine) :
    CapturingAnsiEscapesAwareProcessHandler(commandLine) {

    override fun readerOptions(): BaseOutputReader.Options = BaseOutputReader.Options.BLOCKING

    companion object {
        fun startProcess(commandLine: GeneralCommandLine): RsResult<MvCapturingProcessHandler, ExecutionException> {
            return try {
                RsResult.Ok(MvCapturingProcessHandler(commandLine))
            } catch (e: ExecutionException) {
                RsResult.Err(e)
            }
        }
    }
}
