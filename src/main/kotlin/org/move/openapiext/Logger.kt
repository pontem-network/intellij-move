package org.move.openapiext

import com.intellij.openapi.diagnostic.Logger
import org.move.openapiext.common.isUnitTestMode

fun Logger.infoInProduction(message: String) {
    if (isUnitTestMode) {
        this.warn(message)
    } else {
        this.info(message)
    }
}
