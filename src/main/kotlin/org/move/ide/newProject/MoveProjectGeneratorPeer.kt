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
import org.move.cli.settings.endless.EndlessExecType
import org.move.cli.settings.endless.ChooseEndlessCliPanel
import org.move.stdext.getCliFromPATH
import javax.swing.JComponent

class MoveProjectGeneratorPeer(val parentDisposable: Disposable): GeneratorPeerImpl<EndlessProjectConfig>() {

    private val chooseEndlessCliPanel = ChooseEndlessCliPanel { checkValid?.run() }

    init {
        Disposer.register(parentDisposable, chooseEndlessCliPanel)

        // set values from the default project settings
        val defaultProjectSettings =
            ProjectManager.getInstance().defaultProject.getService(MvProjectSettingsService::class.java)

        val localEndlessPath =
            defaultProjectSettings.endlessPath ?: getCliFromPATH("endless")?.toString()
        chooseEndlessCliPanel.data = ChooseEndlessCliPanel.Data(localEndlessPath)
    }

    private var checkValid: Runnable? = null

    override fun getSettings(): EndlessProjectConfig {
        val endlessPath = this.chooseEndlessCliPanel.data.endlessPath
        if (endlessPath != null) {
            this.chooseEndlessCliPanel.updateEndlessSdks(endlessPath)
        }
        return EndlessProjectConfig(endlessPath)
    }

    override fun getComponent(myLocationField: TextFieldWithBrowseButton, checkValid: Runnable): JComponent {
        this.checkValid = checkValid
        return panel {
            chooseEndlessCliPanel.attachToLayout(this)
        }
    }

    override fun validate(): ValidationInfo? {
        val panelData = this.chooseEndlessCliPanel.data
        val endlessExecPath =
            EndlessExecType.endlessCliPath(panelData.endlessPath)
        if (endlessExecPath == null) {
            return ValidationInfo("Invalid path to Endless executable")
        }
        return null
    }
}
