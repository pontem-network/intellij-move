/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.move.cli.runconfig

import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.util.io.BaseOutputReader
import org.move.stdext.MvResult

class MvCapturingProcessHandler private constructor(commandLine: GeneralCommandLine) :
    CapturingProcessHandler(commandLine) {

    override fun readerOptions(): BaseOutputReader.Options = BaseOutputReader.Options.BLOCKING

    companion object {
        fun startProcess(commandLine: GeneralCommandLine): MvResult<MvCapturingProcessHandler, ExecutionException> {
            return try {
                MvResult.Ok(MvCapturingProcessHandler(commandLine))
            } catch (e: ExecutionException) {
                MvResult.Err(e)
            }
        }
    }
}
