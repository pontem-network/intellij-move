package org.move.cli

object MvConstants {
    const val MANIFEST_FILE = "Move.toml"
    const val ADDR_PLACEHOLDER = "_"

    object ProjectLayout {
        val sources = listOf("sources", "examples")
        val tests = listOf("tests")
        const val build = "build"
    }
}
