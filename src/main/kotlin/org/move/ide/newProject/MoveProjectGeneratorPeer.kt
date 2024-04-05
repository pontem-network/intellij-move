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
import com.intellij.ui.components.JBRadioButton
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.actionListener
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.selected
import org.move.cli.settings.Blockchain
import org.move.cli.settings.MvProjectSettingsService
import org.move.cli.settings.aptos.AptosExecType
import org.move.cli.settings.aptos.ChooseAptosCliPanel
import org.move.cli.settings.isValidExecutable
import org.move.cli.settings.sui.ChooseSuiCliPanel
import org.move.stdext.toPathOrNull
import javax.swing.JComponent

class MoveProjectGeneratorPeer(val parentDisposable: Disposable): GeneratorPeerImpl<MoveProjectConfig>() {

    private val chooseAptosCliPanel = ChooseAptosCliPanel { checkValid?.run() }
    private val chooseSuiCliPanel = ChooseSuiCliPanel { checkValid?.run() }

    private var blockchain: Blockchain

    init {
        Disposer.register(parentDisposable, chooseAptosCliPanel)
        Disposer.register(parentDisposable, chooseSuiCliPanel)

        // set values from the default project settings
        val defaultProjectSettings =
            ProjectManager.getInstance().defaultProject.getService(MvProjectSettingsService::class.java)
        blockchain = defaultProjectSettings.blockchain

        val localAptosPath = defaultProjectSettings.localAptosPath ?: Blockchain.aptosFromPATH()
        val localSuiPath = defaultProjectSettings.localSuiPath ?: Blockchain.suiFromPATH()
        chooseAptosCliPanel.data =
            ChooseAptosCliPanel.Data(defaultProjectSettings.aptosExecType, localAptosPath)
        chooseSuiCliPanel.data = ChooseSuiCliPanel.Data(localSuiPath)
    }

    private var checkValid: Runnable? = null

    override fun getSettings(): MoveProjectConfig {
        return MoveProjectConfig(
            blockchain = blockchain,
            aptosExecType = this.chooseAptosCliPanel.data.aptosExecType,
            localAptosPath = this.chooseAptosCliPanel.data.localAptosPath,
            localSuiPath = this.chooseSuiCliPanel.data.localSuiPath
        )
    }

    override fun getComponent(myLocationField: TextFieldWithBrowseButton, checkValid: Runnable): JComponent {
        this.checkValid = checkValid
        return super.getComponent(myLocationField, checkValid)
    }

    override fun getComponent(): JComponent {
        val generatorPeer = this
        return panel {
            var aptosRadioButton: Cell<JBRadioButton>? = null
            var suiRadioButton: Cell<JBRadioButton>? = null
            buttonsGroup("Blockchain") {
                row {
                    aptosRadioButton = radioButton("Aptos")
                        .selected(generatorPeer.blockchain == Blockchain.APTOS)
                        .actionListener { _, _ ->
                            generatorPeer.blockchain = Blockchain.APTOS
                            checkValid?.run()
                        }
//                        .bindSelected(
//                            { generatorPeer.blockchain == Blockchain.APTOS },
//                            {
//                                generatorPeer.blockchain = Blockchain.APTOS
//                                checkValid?.run()
//                            }
//                        )
                    suiRadioButton = radioButton("Sui")
                        .selected(generatorPeer.blockchain == Blockchain.SUI)
                        .actionListener { _, _ ->
                            generatorPeer.blockchain = Blockchain.SUI
                            checkValid?.run()
                        }
//                        .bindSelected(
//                            { generatorPeer.blockchain == Blockchain.SUI },
//                            {
//                                generatorPeer.blockchain = Blockchain.SUI
//                                checkValid?.run()
//                            }
//                        )
                }
            }

            chooseAptosCliPanel
                .attachToLayout(this)
                .visibleIf(aptosRadioButton!!.selected)
            chooseSuiCliPanel
                .attachToLayout(this)
                .visibleIf(suiRadioButton!!.selected)
        }
    }

    override fun validate(): ValidationInfo? {
        when (blockchain) {
            Blockchain.APTOS -> {
                val panelData = this.chooseAptosCliPanel.data
                val aptosExecPath =
                    AptosExecType.aptosExecPath(panelData.aptosExecType, panelData.localAptosPath)
                if (aptosExecPath == null) {
                    return ValidationInfo("Invalid path to $blockchain executable")
                }
            }
            Blockchain.SUI -> {
                val suiExecPath = this.chooseSuiCliPanel.data.localSuiPath?.toPathOrNull()
                if (suiExecPath == null
                    || !suiExecPath.isValidExecutable()
                ) {
                    return ValidationInfo("Invalid path to $blockchain executable")
                }
            }
        }
        return null
    }
}
