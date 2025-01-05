package org.move.cli.runConfigurations.test

import com.google.common.annotations.VisibleForTesting
import com.intellij.execution.runners.ExecutionEnvironment
import org.move.cli.runConfigurations.AptosCommandLine
import org.move.cli.runConfigurations.AptosRunStateBase
import org.move.cli.runConfigurations.CommandConfigurationBase
import org.move.cli.runConfigurations.aptos.AptosTestConsoleBuilder
import org.move.cli.runConfigurations.aptos.cmd.AptosCommandConfiguration
import org.move.cli.runConfigurations.buildtool.AptosPatch

class AptosTestRunState(
    environment: ExecutionEnvironment,
    cleanConfiguration: CommandConfigurationBase.CleanConfiguration.Ok
): AptosRunStateBase(environment, cleanConfiguration) {

    private val aptosTestPatch: AptosPatch = { commandLine ->
//        val rustcVersion = cargoProject?.rustcInfo?.version
//        // Stable Rust test framework does not support `-Z unstable-options --format json` since 1.70.0-beta
//        // (https://github.com/rust-lang/rust/pull/109044)
//        val requiresRustcBootstrap = !(rustcVersion != null
//                && (rustcVersion.channel == RustChannel.NIGHTLY
//                || rustcVersion.channel == RustChannel.DEV
//                || rustcVersion.semver < RUSTC_1_70_BETA))
//        val environmentVariables = if (requiresRustcBootstrap) {
//            if (!PropertiesComponent.getInstance().getBoolean(DO_NOT_SHOW_KEY, false)) {
//                showRustcBootstrapWarning(project)
//            }
//            val oldVariables = commandLine.environmentVariables
//            EnvironmentVariablesData.create(
//                oldVariables.envs + mapOf(RUSTC_BOOTSTRAP to "1"),
//                oldVariables.isPassParentEnvs
//            )
//        } else {
//            commandLine.environmentVariables
//        }
//
//        // TODO: always pass `withSudo` when `com.intellij.execution.process.ElevationService` supports error stream redirection
//        // https://github.com/intellij-rust/intellij-rust/issues/7320
//        if (commandLine.withSudo) {
//            val message = if (SystemInfo.isWindows) {
//                RsBundle.message("notification.run.tests.as.root.windows")
//            } else {
//                RsBundle.message("notification.run.tests.as.root.unix")
//            }
//            project.showBalloon(message, NotificationType.WARNING)
//        }

        commandLine.copy(
            arguments = patchArgs(commandLine),
//            emulateTerminal = false,
//            withSudo = false
        )
    }

    init {
        consoleBuilder =
            AptosTestConsoleBuilder(environment.runProfile as AptosCommandConfiguration, environment.executor)
        commandLinePatches.add(aptosTestPatch)
        createFilters().forEach { consoleBuilder.addFilter(it) }
    }

//    override fun execute(executor: Executor, runner: ProgramRunner<*>): ExecutionResult {
//        val processHandler = startProcess()
//        val console = createConsole(executor)
//        console?.attachToProcess(processHandler)
//        return DefaultExecutionResult(console, processHandler).apply { setRestartActions(ToggleAutoTestAction()) }
//    }

    companion object {
        @VisibleForTesting
        fun patchArgs(commandLine: AptosCommandLine): List<String> {
//            val (pre, post) = commandLine.splitOnDoubleDash()
//                .let { (pre, post) -> pre.toMutableList() to post.toMutableList() }

            val args = commandLine.arguments.toMutableList()
//            val noFailFastOption = "--no-fail-fast"
//            if (noFailFastOption !in pre) {
//                pre.add(noFailFastOption)
//            }

//            val unstableOption = "-Z"
//            if (unstableOption !in post) {
//                post.add(unstableOption)
//                post.add("unstable-options")
//            }

            if ("--format-json" !in args) {
                args.add("--format-json")
            }

//            addFormatJsonOption(post, "--format", "json")
//            post.add("--show-output")

            return args
        }
    }
}
