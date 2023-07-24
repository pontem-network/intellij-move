/*
 * MIT License
 *
 * Copyright (c) 2020-2020 JetBrains s.r.o.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.move.ide.actions.download.ui

import com.intellij.execution.Platform
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.validation.DialogValidation
import com.intellij.ui.dsl.builder.*
import org.move.ide.actions.download.DownloadAptosTask
import org.move.ide.actions.download.PathSelector
import java.io.File
import javax.swing.JComponent

class DownloadAptosDialog(
    private val parentComponent: JComponent,
) : DialogWrapper(null, parentComponent, true, IdeModalityType.IDE) {

    var outPath: String? = null

    private var version = "2.0.2"
    private var downloadToProjectRoot: Boolean = true
    private var customDestinationDirectory: String = ""

    init {
        init()
        title = "Download Aptos"
        setOKButtonText("Download")
    }

    override fun createCenterPanel(): JComponent {
        return panel {
            row("Version") {
                textField()
                    .bindText(
                        { version },
                        { version = it }
                    )
                comment("{major}.{minor}.{patch}, i.e. 1.9.0, 2.0.2")
            }
            buttonsGroup("Download To") {
                row {
                    radioButton("Home directory")
                        .bindSelected(
                            { downloadToProjectRoot },
                            { downloadToProjectRoot = it }
                        )
                    val sep = fileSeparator()
                    comment("Download to the \$HOME\$${sep}aptos-cli${sep} directory")
                }
                row {
                    val radio = radioButton("Other")
                    radio
                        .bindSelected(
                            { !downloadToProjectRoot },
                            { downloadToProjectRoot = !it }
                        )
                    textFieldWithBrowseButton("Choose Directory")
                        .enabledIf(radio.selected)
                        .bindText(
                            { customDestinationDirectory },
                            { customDestinationDirectory = it }
                        )
                }
            }
        }
    }

    override fun getDimensionServiceKey() = DownloadAptosDialog::class.simpleName

    override fun doOKAction() {
        super.doOKAction()

        val destinationDir = when (downloadToProjectRoot) {
            true -> {
                val dir = PathSelector.getHomeAptosFolder()
                val file = File(dir)
                if (!file.exists()) {
                    if (!file.mkdir()) {
                        // TODO: show error
//                        SwingUtilities.invokeLater {
//                            Messages.showErrorDialog(
//                                project,
//                                "Cannot create $dir directory",
//                                "IO Error",
//                            )
//                        }
                        return
                    }
                }
                dir
            }
            false -> customDestinationDirectory
        }
        val downloadTask = DownloadAptosTask(parentComponent, version, destinationDir)
        downloadTask.queue()

        outPath = downloadTask.result
    }

    companion object {
        fun fileSeparator(): String = Platform.current().fileSeparator.toString()
    }
}
