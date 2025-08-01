/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.move.ide.newProject

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.util.Disposer
import com.intellij.platform.GeneratorPeerImpl
import com.intellij.ui.dsl.builder.panel
import org.move.cli.settings.MvProjectSettingsService
import org.move.cli.settings.aptos.AptosExecType
import org.move.cli.settings.aptos.ChooseAptosCliPanel
import org.move.stdext.getCliFromPATH
import javax.swing.JComponent

class MoveProjectGeneratorPeer(val parentDisposable: Disposable): GeneratorPeerImpl<AptosProjectConfig>() {

    private val chooseAptosCliPanel = ChooseAptosCliPanel { checkValid?.run() }

    init {
        Disposer.register(parentDisposable, chooseAptosCliPanel)

        // set values from the default project settings
        val defaultProjectSettings =
            ProjectManager.getInstance().defaultProject.getService(MvProjectSettingsService::class.java)

        val localAptosPath =
            defaultProjectSettings.aptosPath ?: getCliFromPATH("aptos")?.toString()
        chooseAptosCliPanel.data = ChooseAptosCliPanel.Data(localAptosPath)
    }

    private var checkValid: Runnable? = null

    override fun getSettings(): AptosProjectConfig {
        val aptosPath = this.chooseAptosCliPanel.data.aptosPath
        if (aptosPath != null) {
            this.chooseAptosCliPanel.updateAptosSdks(aptosPath)
        }
        return AptosProjectConfig(aptosPath)
    }

    override fun getComponent(myLocationField: TextFieldWithBrowseButton, checkValid: Runnable): JComponent {
        this.checkValid = checkValid
        return panel {
            chooseAptosCliPanel.attachToLayout(this)
        }
    }

    override fun validate(): ValidationInfo? {
        val panelData = this.chooseAptosCliPanel.data
        val aptosExecPath =
            AptosExecType.aptosCliPath(panelData.aptosPath)
        if (aptosExecPath == null) {
            return ValidationInfo("Invalid path to Aptos executable")
        }
        return null
    }
}
