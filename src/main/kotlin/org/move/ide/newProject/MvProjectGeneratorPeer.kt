/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.move.ide.newProject

import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.platform.GeneratorPeerImpl
import com.intellij.ui.components.JBTextField
import com.intellij.ui.layout.panel
import org.move.cli.settings.AptosSettingsPanel
import org.move.cli.settings.isValidExecutable
import org.move.stdext.toPathOrNull
import javax.swing.JComponent

class MvProjectGeneratorPeer : GeneratorPeerImpl<ConfigurationData>() {

    private val aptosSettingsPanel = AptosSettingsPanel { checkValid?.run() }
//    private val privateKeyTextField = JBTextField("")

    private var checkValid: Runnable? = null

    override fun getSettings(): ConfigurationData = aptosSettingsPanel.data

    override fun getComponent(myLocationField: TextFieldWithBrowseButton, checkValid: Runnable): JComponent {
        this.checkValid = checkValid
        return super.getComponent(myLocationField, checkValid)
    }

    override fun getComponent(): JComponent = panel {
        titledRow("") {}
        aptosSettingsPanel.attachTo(this)
//        titledRow("") {
//            row("Private key") { privateKeyTextField() }
//        }
    }

    override fun validate(): ValidationInfo? {
        val aptosPath = this.aptosSettingsPanel.data.aptosPath.toPathOrNull()
        if (aptosPath == null || !aptosPath.isValidExecutable()) {
            return ValidationInfo("Invalid path to Aptos executable")
        }
        return null
    }
}
