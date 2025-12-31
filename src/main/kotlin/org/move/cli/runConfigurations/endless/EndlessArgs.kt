package org.move.cli.runConfigurations.endless

import com.intellij.util.execution.ParametersListUtil

data class EndlessArgs(val args: MutableList<String> = mutableListOf<String>()) {
    fun enableCompilerJson() {
        args.add("--experiments")
        args.add("compiler-message-format-json")
    }

    fun applyExtra(builder: (MutableList<String>).() -> Unit): EndlessArgs {
        val extraArgs = mutableListOf<String>()
        extraArgs.builder()
        this.applyExtra(extraArgs)
        return this
    }

    fun applyExtra(extraArgs: List<String>): EndlessArgs {
        for (arg in extraArgs) {
            if (arg !in this.args) {
                args.add(arg)
            }
        }
        return this
    }

    fun applyExtra(extraArgsLine: String): EndlessArgs {
        val extraArguments = ParametersListUtil.parse(extraArgsLine)
        this.applyExtra(extraArguments)
        return this
    }
}