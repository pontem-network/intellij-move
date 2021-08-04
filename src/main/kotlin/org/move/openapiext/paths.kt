package org.move.openapiext

import com.intellij.util.io.exists
import java.nio.file.Path
import java.nio.file.Paths

fun Path.resolveAbsPath(other: String): Path? {
    val rawPath = Paths.get(other)
    if (rawPath.isAbsolute && rawPath.exists()) return rawPath

    val joined = this.resolve(other)
    if (!joined.exists()) {
        return null
    } else {
        return joined.toRealPath()
    }

}
