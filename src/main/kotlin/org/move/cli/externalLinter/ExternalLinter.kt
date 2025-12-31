/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.move.cli.externalLinter

enum class ExternalLinter(val title: String, val command: String) {
    COMPILER("Endless Compiler", "compile"),
    LINTER("Endless Linter", "lint");

    override fun toString(): String = title

    companion object {
        @JvmField
        val DEFAULT: ExternalLinter = COMPILER
    }
}
