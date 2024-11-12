package org.move.openapiext

import com.intellij.execution.process.AnsiEscapeDecoder
import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessOutput
import com.intellij.openapi.util.Key

val ProcessOutput.isSuccess: Boolean get() = !isTimeout && !isCancelled && exitCode == 0

/**
 * Capturing adapter that removes ANSI escape codes from the output
 */
abstract class AnsiEscapedProcessAdapter: ProcessAdapter(), AnsiEscapeDecoder.ColoredTextAcceptor {
    private val decoder = AnsiEscapeDecoder()
    private val eolType: Key<Any> = Key.create("end of line")

    override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
        decoder.escapeText(event.text, outputType, this)
        decoder.escapeText("", eolType, this)
    }

    var buffer = StringBuilder()

    override fun coloredTextAvailable(text: String, attributes: Key<*>) {
        if (attributes == eolType) {
            onColoredTextAvailable(buffer.toString())
            buffer = StringBuilder()
        } else {
            buffer.append(text)
        }
    }

    abstract fun onColoredTextAvailable(text: String)
}

