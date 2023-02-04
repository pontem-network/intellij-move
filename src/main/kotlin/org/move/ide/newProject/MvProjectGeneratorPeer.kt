/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.move.ide.newProject

import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.platform.GeneratorPeerImpl
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.dsl.builder.BottomGap
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import com.intellij.ui.layout.selected
import org.move.cli.Aptos
import org.move.cli.settings.AptosSettingsPanel
import org.move.cli.settings.MoveSettingsPanel
import org.move.cli.settings.isValidExecutable
import org.move.stdext.toPathOrNull
import javax.swing.JComponent

class MvProjectGeneratorPeer : GeneratorPeerImpl<NewProjectData>() {

    private val moveSettingsPanel =
        MoveSettingsPanel(showDefaultSettingsLink = true) { checkValid?.run() }

    private val aptosInitCheckBox = JBCheckBox("Run 'aptos init'", false)
    private val aptosSettingsPanel = AptosSettingsPanel(aptosInitCheckBox.selected)

    private var checkValid: Runnable? = null

    override fun getSettings(): NewProjectData {
        return NewProjectData(
            data = moveSettingsPanel.data,
            aptosInitEnabled = aptosInitCheckBox.isSelected,
            initData = aptosSettingsPanel.data
        )
    }

    override fun getComponent(myLocationField: TextFieldWithBrowseButton, checkValid: Runnable): JComponent {
        this.checkValid = checkValid
        return super.getComponent(myLocationField, checkValid)
    }

    override fun getComponent(): JComponent {
        val panel = panel {
            group {}
            moveSettingsPanel.attachTo(this)
            group {}.bottomGap(BottomGap.MEDIUM)

            row { cell(aptosInitCheckBox).horizontalAlign(HorizontalAlign.FILL) }
            aptosSettingsPanel.attachTo(this)
        }
        val suggestedAptosPath = Aptos.suggestPath()
        if (suggestedAptosPath != null) {
            moveSettingsPanel.data = MoveSettingsPanel.Data(suggestedAptosPath)
        }
        return panel
    }

    override fun validate(): ValidationInfo? {
        val aptosPath = this.moveSettingsPanel.data.aptosPath.toPathOrNull()
        if (aptosPath == null || !aptosPath.isValidExecutable()) {
            return ValidationInfo("Invalid path to Aptos executable")
        }

        if (aptosInitEnabled()) {
            return aptosSettingsPanel.validate()
        }
        return null
    }

    private fun aptosInitEnabled(): Boolean {
        return aptosInitCheckBox.isSelected
    }
}
