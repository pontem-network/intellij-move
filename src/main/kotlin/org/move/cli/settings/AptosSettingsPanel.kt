package org.move.cli.settings

import com.intellij.openapi.Disposable
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.util.Disposer
import com.intellij.ui.components.JBTextField
import com.intellij.ui.layout.ComponentPredicate
import com.intellij.ui.layout.LayoutBuilder
import com.intellij.ui.layout.enableIf
import org.move.openapiext.pathField

class AptosSettingsPanel(val panelEnabled: ComponentPredicate) : Disposable {
    private val privateKeyPathField =
        pathField(
            FileChooserDescriptorFactory.createSingleFileOrExecutableAppDescriptor(),
            this,
            "Private key file"
        )
    private val faucetUrlField = JBTextField()
    private val restUrlField = JBTextField()

    data class InitData(
        val privateKeyPath: String,
        val faucetUrl: String,
        val restUrl: String
    )

    var data: InitData
        get() {
            return InitData(privateKeyPathField.text, faucetUrlField.text, restUrlField.text)
        }
        set(value) {
            privateKeyPathField.text = value.privateKeyPath
            faucetUrlField.text = value.faucetUrl
            restUrlField.text = value.restUrl
        }

    fun attachTo(layout: LayoutBuilder) = with(layout) {
        row("Private key file") { privateKeyPathField() }.enableIf(panelEnabled)
        row("Faucet URL") { faucetUrlField() }.enableIf(panelEnabled)
        row("Rest API URL") { restUrlField()}.enableIf(panelEnabled)
    }

    override fun dispose() {
        Disposer.dispose(privateKeyPathField)
    }
}
