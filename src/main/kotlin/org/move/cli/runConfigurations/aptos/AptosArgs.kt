package org.move.cli.runConfigurations.aptos

import com.intellij.util.execution.ParametersListUtil

data class AptosArgs(val args: MutableList<String> = mutableListOf<String>()) {
    fun enableCompilerJson() {
        args.add("--experiments")
        args.add("compiler-message-format-json")
    }

    fun applyExtra(builder: (MutableList<String>).() -> Unit): AptosArgs {
        val extraArgs = mutableListOf<String>()
        extraArgs.builder()
        this.applyExtra(extraArgs)
        return this
    }

    fun applyExtra(extraArgs: List<String>): AptosArgs {
        for (arg in extraArgs) {
            if (arg !in this.args) {
                args.add(arg)
            }
        }
        return this
    }

    fun applyExtra(extraArgsLine: String): AptosArgs {
        val extraArguments = ParametersListUtil.parse(extraArgsLine)
        this.applyExtra(extraArguments)
        return this
    }
}