package org.move.cli.runconfig

interface MoveBinaryCommandConfig {
    fun name(): String
    fun commandLine(): MoveCommandLine
}
