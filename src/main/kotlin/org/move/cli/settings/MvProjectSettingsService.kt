package org.move.cli.settings

import com.intellij.execution.configurations.PathEnvironmentVariableUtil
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.registry.Registry
import org.move.cli.runConfigurations.BlockchainCli
import org.move.cli.runConfigurations.aptos.Aptos
import org.move.cli.runConfigurations.sui.Sui
import org.move.cli.settings.Blockchain.APTOS
import org.move.cli.settings.Blockchain.SUI
import org.move.cli.settings.MvProjectSettingsService.MoveProjectSettings
import org.move.cli.settings.aptos.AptosExecType
import org.move.stdext.exists
import org.move.stdext.isExecutableFile
import org.move.stdext.toPathOrNull
import java.nio.file.Path

enum class Blockchain {
    APTOS, SUI;

    fun cliName() = when (this) {
        SUI -> "sui"; APTOS -> "aptos"
    }

    override fun toString(): String = if (this == APTOS) "Aptos" else "Sui"

    companion object {
        fun aptosCliFromPATH(): Path? = blockchainCliFromPATH("aptos")
        fun suiCliFromPATH(): Path? = blockchainCliFromPATH("sui")

        fun blockchainCliFromPATH(cliName: String): Path? {
            return PathEnvironmentVariableUtil
                .findExecutableInPathOnAnyOS(cliName)
                ?.toPath()?.toAbsolutePath()
        }

    }
}

val Project.moveSettings: MvProjectSettingsService get() = service()

private const val SERVICE_NAME: String = "MoveProjectSettingsService_1"

@State(
    name = SERVICE_NAME,
    storages = [Storage(StoragePathMacros.WORKSPACE_FILE)]
)
class MvProjectSettingsService(
    project: Project
):
    MvProjectSettingsServiceBase<MoveProjectSettings>(project, MoveProjectSettings()) {

    val blockchain: Blockchain get() = state.blockchain

    val aptosExecType: AptosExecType get() = state.aptosExecType
    val localAptosPath: String? get() = state.localAptosPath
    val localSuiPath: String? get() = state.localSuiPath

    val isCompilerV2: Boolean get() = state.isCompilerV2
    val fetchAptosDeps: Boolean get() = state.fetchAptosDeps

    val disableTelemetry: Boolean get() = state.disableTelemetry
    val skipFetchLatestGitDeps: Boolean get() = state.skipFetchLatestGitDeps
    val dumpStateOnTestFailure: Boolean get() = state.dumpStateOnTestFailure

    // default values for settings
    class MoveProjectSettings: MvProjectSettingsBase<MoveProjectSettings>() {
        @AffectsMoveProjectsMetadata
        var blockchain: Blockchain by enum(APTOS)

        @AffectsMoveProjectsMetadata
        var aptosExecType: AptosExecType by enum(defaultAptosExecType)

        @AffectsMoveProjectsMetadata
        var localAptosPath: String? by string()

        @AffectsMoveProjectsMetadata
        var localSuiPath: String? by string()

        @AffectsParseTree
        var isCompilerV2: Boolean by property(false)

        @AffectsMoveProjectsMetadata
        var fetchAptosDeps: Boolean by property(false)

        var disableTelemetry: Boolean by property(true)

        // change to true here to not annoy the users with constant updates
        var skipFetchLatestGitDeps: Boolean by property(true)
        var dumpStateOnTestFailure: Boolean by property(false)

        override fun copy(): MoveProjectSettings {
            val state = MoveProjectSettings()
            state.copyFrom(this)
            return state
        }
    }

    override fun createSettingsChangedEvent(
        oldEvent: MoveProjectSettings,
        newEvent: MoveProjectSettings
    ): SettingsChangedEvent = SettingsChangedEvent(oldEvent, newEvent)

    class SettingsChangedEvent(
        oldState: MoveProjectSettings,
        newState: MoveProjectSettings
    ): SettingsChangedEventBase<MoveProjectSettings>(oldState, newState)

    companion object {
        private val defaultAptosExecType
            get() =
                if (AptosExecType.isPreCompiledSupportedForThePlatform) AptosExecType.BUNDLED else AptosExecType.LOCAL
    }
}

val Project.blockchain: Blockchain get() = this.moveSettings.blockchain

fun Project.getBlockchainCli(blockchain: Blockchain, parentDisposable: Disposable? = null): BlockchainCli? {
    return when (blockchain) {
        APTOS -> {
            val aptosExecPath =
                AptosExecType.aptosExecPath(
                    this.moveSettings.aptosExecType,
                    this.moveSettings.localAptosPath
                )
            aptosExecPath?.let { Aptos(it, parentDisposable) }
        }
        SUI -> this.moveSettings.localSuiPath?.toPathOrNull()?.let { Sui(it, parentDisposable) }
    }
}

fun Project.getBlockchainCli(parentDisposable: Disposable?): BlockchainCli? =
    getBlockchainCli(this.blockchain, parentDisposable)

val Project.isAptosConfigured: Boolean get() = getBlockchainCli(APTOS) != null
fun Project.getAptosCli(parentDisposable: Disposable? = null): Aptos? = getBlockchainCli(APTOS, parentDisposable) as? Aptos

fun Project.getSuiCli(parentDisposable: Disposable? = null): Sui? =
    getBlockchainCli(SUI, parentDisposable) as? Sui

val Project.aptosExecPath: Path? get() = this.getAptosCli()?.cliLocation

val Project.suiExecPath: Path? get() = this.getSuiCli()?.cliLocation

fun Path?.isValidExecutable(): Boolean {
    return this != null
            && this.toString().isNotBlank()
            && this.exists()
            && this.isExecutableFile()
}

fun isDebugModeEnabled(): Boolean = Registry.`is`("org.move.debug.enabled")

fun <T> debugErrorOrFallback(message: String, fallback: T): T {
    if (isDebugModeEnabled()) {
        error(message)
    }
    return fallback
}

fun <T> debugErrorOrFallback(message: String, cause: Throwable?, fallback: () -> T): T {
    if (isDebugModeEnabled()) {
        throw IllegalStateException(message, cause)
    }
    return fallback()
}
