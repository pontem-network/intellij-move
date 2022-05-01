package org.move.cli.settings

import com.intellij.openapi.Disposable
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.util.Disposer
import com.intellij.ui.components.JBTextField
import com.intellij.ui.layout.LayoutBuilder
import org.move.cli.Aptos
import org.move.openapiext.UiDebouncer
import org.move.openapiext.pathTextField
import org.move.stdext.toPathOrNull
import java.nio.file.Path

class MoveProjectSettingsPanel(
    private val updateListener: (() -> Unit)? = null
) : Disposable {
    private val versionUpdateDebouncer = UiDebouncer(this)

    //    private val pathToAptosCombobox = MvAptosPathChoosingComboBox { update() }
    private val pathToAptosTextField =
        pathTextField(
            FileChooserDescriptorFactory.createSingleFileOrExecutableAppDescriptor(),
            this,
            "Select Aptos path"
        ) { update() }

    //    private val pathToAptosCombobox = MvAptosPathChoosingComboBox { update() }
    private val versionLabel = VersionLabel()
    private val privateKeyTextField = JBTextField()

    data class Data(
        val aptosPath: Path?,
        val privateKey: String
    )

    var data: Data
        get() {
            val aptosPath = pathToAptosTextField.text.toPathOrNull()
            val privateKey = privateKeyTextField.text
            return Data(aptosPath, privateKey)
        }
        set(value) {
            pathToAptosTextField.text = value.aptosPath?.toString().orEmpty()
            privateKeyTextField.text = value.privateKey
            update()
        }

    fun attachTo(layout: LayoutBuilder) = with(layout) {
        // Don't use `Project.toolchain` or `Project.rustSettings` here because
        // `getService` can return `null` for default project after dynamic plugin loading.
        // As a result, you can get `java.lang.IllegalStateException`
        // So let's handle it manually
        val projectSettings =
            ProjectManager.getInstance().defaultProject.getService(MoveProjectSettingsService::class.java)
        data = Data(
            aptosPath = projectSettings?.aptosPath,
            privateKey = projectSettings.settingsState.privateKey
        )
        row("Aptos CLI") {
            pathToAptosTextField()
            comment("(required)")
        }
        row("Version") { versionLabel() }
        row("Private key") { wrapComponent(privateKeyTextField)(growX, pushX) }
    }

    private fun update() {
        val aptosPath = pathToAptosTextField.text.toPathOrNull()
        versionUpdateDebouncer.run(
            onPooledThread = {
                val aptosVersion = aptosPath?.let { Aptos(it) }?.version()
                aptosVersion
            },
            onUiThread = { version ->
                versionLabel.setVersion(version)
                updateListener?.invoke()
            }
        )
    }

    override fun dispose() {
        Disposer.dispose(pathToAptosTextField)
    }
}
