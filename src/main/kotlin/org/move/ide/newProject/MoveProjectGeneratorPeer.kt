/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.move.ide.newProject

import com.intellij.openapi.Disposable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.util.Disposer
import com.intellij.platform.GeneratorPeerImpl
import com.intellij.ui.components.JBRadioButton
import com.intellij.ui.dsl.builder.*
import org.move.cli.runConfigurations.InitProjectCli
import org.move.cli.settings.Blockchain
import org.move.cli.settings.aptos.ChooseAptosCliPanel
import org.move.cli.settings.isValidExecutable
import org.move.cli.settings.sui.ChooseSuiCliPanel
import org.move.stdext.toPathOrNull
import javax.swing.JComponent

class MoveProjectGeneratorPeer(val parentDisposable: Disposable): GeneratorPeerImpl<MoveProjectConfig>() {

    private val chooseAptosCliPanel = ChooseAptosCliPanel { checkValid?.run() }
    private val chooseSuiCliPanel = ChooseSuiCliPanel { checkValid?.run() }

    init {
        Disposer.register(parentDisposable, chooseAptosCliPanel)
        Disposer.register(parentDisposable, chooseSuiCliPanel)
    }

    private var checkValid: Runnable? = null
    private var blockchain: Blockchain = Blockchain.SUI

    override fun getSettings(): MoveProjectConfig {
        val initCli =
            when (blockchain) {
                Blockchain.APTOS -> {
                    InitProjectCli.Aptos(this.chooseAptosCliPanel.selectedAptosExec)
                }
                Blockchain.SUI -> {
                    val suiPath = this.chooseSuiCliPanel.getSuiCliPath().toPathOrNull()
                        ?: error("Should be validated separately")
                    InitProjectCli.Sui(suiPath)
                }
            }
        return MoveProjectConfig(blockchain, initCli)
    }

    override fun getComponent(myLocationField: TextFieldWithBrowseButton, checkValid: Runnable): JComponent {
        this.checkValid = checkValid
        return super.getComponent(myLocationField, checkValid)
    }

    override fun getComponent(): JComponent {
        return panel {
            var aptosRadioButton: Cell<JBRadioButton>? = null
            var suiRadioButton: Cell<JBRadioButton>? = null

            buttonsGroup("Blockchain") {
                row {
                    aptosRadioButton = radioButton("Aptos", Blockchain.APTOS)
                        .actionListener { _, _ ->
                            blockchain = Blockchain.APTOS
                            checkValid?.run()
                        }
                    suiRadioButton = radioButton("Sui", Blockchain.SUI)
                        .actionListener { _, _ ->
                            blockchain = Blockchain.SUI
                            checkValid?.run()
                        }
                }
            }
                .bind({ blockchain }, { blockchain = it })

            chooseAptosCliPanel.attachToLayout(this)
                .visibleIf(aptosRadioButton!!.selected)
            chooseSuiCliPanel.attachToLayout(this)
                .visibleIf(suiRadioButton!!.selected)
        }
    }

    override fun validate(): ValidationInfo? {
        when (blockchain) {
            Blockchain.APTOS -> {
                val aptosPath = this.chooseAptosCliPanel.selectedAptosExec.toPathOrNull()
                if (aptosPath == null || !aptosPath.isValidExecutable()) {
                    return ValidationInfo("Invalid path to $blockchain executable")
                }
            }
            Blockchain.SUI -> {
                val suiPath = this.chooseSuiCliPanel.getSuiCliPath().toPathOrNull()
                if (suiPath == null || !suiPath.isValidExecutable()) {
                    return ValidationInfo("Invalid path to $blockchain executable")
                }
            }
        }
        return null
    }
}
