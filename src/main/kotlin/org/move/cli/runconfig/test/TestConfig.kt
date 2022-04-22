package org.move.cli.runconfig.test

import org.move.cli.runconfig.MoveBinaryCommandConfig
import org.move.cli.runconfig.MoveCommandLine
import java.nio.file.Path

sealed interface TestConfig: MoveBinaryCommandConfig {
    data class Function(
        val packageName: String,
        val functionName: String,
        val modName: String,
        val path: Path
    ) : TestConfig {
        override fun name() = "Test $packageName: $modName::$functionName"
        override fun commandLine() = MoveCommandLine(
            "package test --filter ${this.functionName}",
            this.path
        )
    }

    data class Package(val packageName: String, val path: Path) : TestConfig {
        override fun name() = "Test $packageName"
        override fun commandLine() = MoveCommandLine("package test", this.path)
    }

    data class Module(val packageName: String, val modName: String, val path: Path) : TestConfig {
        override fun name() = "Test $packageName: $modName"
        override fun commandLine() = MoveCommandLine("package test --filter $modName", this.path)
    }
}
