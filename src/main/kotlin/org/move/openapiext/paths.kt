package org.move.openapiext

import com.intellij.util.io.exists
import java.nio.file.Path

fun Path.resolveAbsPath(other: String): Path? {
    val joined = this.resolve(other)
    if (!joined.exists()) {
        return null
    } else {
        return joined.toRealPath()
    }

}
