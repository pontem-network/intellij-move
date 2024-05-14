package org.move.openapiext

import com.intellij.openapi.diagnostic.LogLevel
import com.intellij.openapi.diagnostic.LogLevel.*
import com.intellij.openapi.diagnostic.Logger
import org.move.openapiext.common.isUnitTestMode

fun Logger.debugInProduction(message: String) {
    if (isUnitTestMode) {
        this.warn(message)
    } else {
        this.debug(message)
    }
}

fun Logger.log(content: String, level: LogLevel) {
    when (level) {
        ERROR -> this.error(content)
        WARNING -> this.warn(content)
        INFO -> this.info(content)
        DEBUG -> this.debug(content)
        TRACE -> this.trace(content)
        OFF, ALL -> {}
    }
}