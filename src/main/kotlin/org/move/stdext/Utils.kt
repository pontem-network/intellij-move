package org.move.stdext

import java.nio.file.Files
import java.nio.file.Path

/**
 * Just a way to force exhaustiveness analysis for Kotlin's `when` expression.
 *
 * Use it like this:
 * ```
 * when (foo) {
 *     is Bar -> {}
 *     is Baz -> {}
 * }.exhaustive // ensure `Bar` and `Baz` are the only variants of `foo`
 * ```
 */
val <T> T.exhaustive: T
    inline get() = this

fun Path.isExecutable(): Boolean = Files.isExecutable(this)
