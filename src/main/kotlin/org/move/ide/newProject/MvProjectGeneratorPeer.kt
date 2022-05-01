/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.move.ide.newProject

import com.intellij.platform.GeneratorPeerImpl
import com.intellij.ui.layout.panel
import org.move.cli.settings.MoveProjectSettingsPanel
import javax.swing.JComponent

class MvProjectGeneratorPeer : GeneratorPeerImpl<ConfigurationData>() {

    private val newProjectPanel = MoveProjectSettingsPanel()

    override fun getSettings(): ConfigurationData = newProjectPanel.data

    override fun getComponent(): JComponent = panel {
        titledRow("") {}
        newProjectPanel.attachTo(this)
    }
}
