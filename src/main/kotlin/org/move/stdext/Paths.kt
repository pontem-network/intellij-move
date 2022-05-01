package org.move.stdext

import com.intellij.openapi.diagnostic.Logger
import java.nio.file.Files
import java.nio.file.InvalidPathException
import java.nio.file.Path
import java.nio.file.Paths

private val LOG = Logger.getInstance("#org.move.stdext")

fun String.toPath(): Path = Paths.get(this)

fun String.toPathOrNull(): Path? = pathOrNull(this::toPath)

fun Path.resolveOrNull(other: String): Path? = pathOrNull { resolve(other) }

private inline fun pathOrNull(block: () -> Path): Path? {
    return try {
        block()
    } catch (e: InvalidPathException) {
        LOG.warn(e)
        null
    }
}

fun Path.isExecutable(): Boolean = Files.isExecutable(this)

//fun Path.list(): Sequence<Path> = Files.list(this).asSequence()
