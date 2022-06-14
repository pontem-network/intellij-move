package org.move.cli.settings

import com.intellij.openapi.Disposable
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.util.Disposer
import com.intellij.ui.components.JBTextField
import com.intellij.ui.layout.ComponentPredicate
import com.intellij.ui.layout.LayoutBuilder
import com.intellij.ui.layout.enableIf
import com.intellij.util.Urls
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

    fun validate(): ValidationInfo? {
        if (data.privateKeyPath.isBlank()) return ValidationInfo("Private key is required")
        if (data.faucetUrl.isBlank()) return ValidationInfo("Faucet url is required")
        if (!data.faucetUrl.isValidUrl()) return ValidationInfo("Faucet url is invalid")
        if (data.restUrl.isBlank()) return ValidationInfo("Rest url is required")
        if (!data.restUrl.isValidUrl()) return ValidationInfo("Rest url is invalid")
        return null
    }
}

private fun String.isValidUrl(): Boolean {
    return Urls.parse(this, false) != null
}
