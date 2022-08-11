package org.move.cli.settings

import com.intellij.openapi.Disposable
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.util.Disposer
import com.intellij.ui.layout.CCFlags
import com.intellij.ui.layout.LayoutBuilder
import org.move.cli.Aptos
import org.move.openapiext.UiDebouncer
import org.move.openapiext.pathField
import org.move.stdext.toPathOrNull

class MoveSettingsPanel(
    private val updateListener: (() -> Unit)? = null
) : Disposable {
    private val versionUpdateDebouncer = UiDebouncer(this)

    private val aptosPathField =
        pathField(
            FileChooserDescriptorFactory.createSingleFileOrExecutableAppDescriptor(),
            this,
            "Aptos binary"
        ) { update() }
    private val versionLabel = VersionLabel()

    data class Data(val aptosPath: String) {
        fun aptos(): Aptos? = aptosPath.toPathOrNull()?.let { Aptos(it) }
    }

    var data: Data
        get() {
            return Data(aptosPathField.text)
        }
        set(value) {
            aptosPathField.text = value.aptosPath
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
            aptosPath = projectSettings.settingsState.aptosPath,
        )
        row("Aptos CLI") {
            wrapComponent(aptosPathField)(CCFlags.growX, CCFlags.pushX)
            comment("(required)")
        }
        row("Version") { versionLabel() }
    }

    private fun update() {
        val aptosPath = aptosPathField.text.toPathOrNull()
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
        Disposer.dispose(aptosPathField)
    }
}
