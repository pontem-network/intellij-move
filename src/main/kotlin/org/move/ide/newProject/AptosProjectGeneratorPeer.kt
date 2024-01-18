/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.move.ide.newProject

import com.intellij.openapi.Disposable
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.util.Disposer
import com.intellij.platform.GeneratorPeerImpl
import com.intellij.ui.dsl.builder.panel
import org.move.cli.settings.aptos.ChooseAptosCliPanel
import org.move.cli.settings.isValidExecutable
import javax.swing.JComponent

class AptosProjectGeneratorPeer(val parentDisposable: Disposable): GeneratorPeerImpl<AptosProjectConfig>() {

    private val chooseAptosCliPanel = ChooseAptosCliPanel { checkValid?.run() }

    init {
        Disposer.register(parentDisposable, chooseAptosCliPanel)
    }

    private var checkValid: Runnable? = null

    override fun getSettings(): AptosProjectConfig {
        return AptosProjectConfig(aptosExec = chooseAptosCliPanel.selectedAptosExec)
    }

    override fun getComponent(myLocationField: TextFieldWithBrowseButton, checkValid: Runnable): JComponent {
        this.checkValid = checkValid
        return super.getComponent(myLocationField, checkValid)
    }

    override fun getComponent(): JComponent {
        return panel {
            chooseAptosCliPanel.attachToLayout(this)
        }
    }

    override fun validate(): ValidationInfo? {
        val aptosPath = this.chooseAptosCliPanel.selectedAptosExec.toPathOrNull()
        if (aptosPath == null || !aptosPath.isValidExecutable()) {
            return ValidationInfo("Invalid path to Aptos executable")
        }
        return null
    }
}
