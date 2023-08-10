package org.move.cli.settings

import com.intellij.openapi.Disposable
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.util.Disposer
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.layout.ComponentPredicate
import com.intellij.util.Urls
import org.move.openapiext.pathField

class AptosInitSettingsPanel(val panelEnabled: ComponentPredicate) : Disposable {
    private val privateKeyPathField =
        pathField(
            FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor(),
            this,
            "Private Key File"
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

    fun attachTo(layout: Panel) = with(layout) {
        row("Private key file") { cell(privateKeyPathField).align(AlignX.FILL) }
            .enabledIf(panelEnabled)
//        row("Private key file") { cell(privateKeyPathField).horizontalAlign(HorizontalAlign.FILL) }
        row("Faucet URL") { cell(faucetUrlField).align(AlignX.FILL) }
            .enabledIf(panelEnabled)
//        row("Faucet URL") { cell(faucetUrlField).horizontalAlign(HorizontalAlign.FILL) }
        row("Rest API URL") { cell(restUrlField).align(AlignX.FILL) }
            .enabledIf(panelEnabled)
//        row("Rest API URL") { cell(restUrlField).horizontalAlign(HorizontalAlign.FILL) }
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
