package org.move.openapiext

import com.intellij.execution.process.ProcessOutput

val ProcessOutput.isSuccess: Boolean get() = !isTimeout && !isCancelled && exitCode == 0
