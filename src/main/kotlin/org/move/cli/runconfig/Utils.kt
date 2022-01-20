package org.move.cli.runconfig

fun MoveCommandLine.mergeWithDefault(default: MoveRunConfiguration): MoveCommandLine =
    copy(
        environmentVariables = default.env,
    )
