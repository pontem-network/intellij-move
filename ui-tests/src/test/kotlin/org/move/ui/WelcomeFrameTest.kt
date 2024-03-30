package org.move.ui

import com.intellij.openapi.util.io.toCanonicalPath
import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.utils.waitFor
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.io.TempDir
import org.move.ui.fixtures.*
import org.move.ui.utils.RemoteRobotExtension
import org.move.ui.utils.StepsLogger
import java.io.File
import java.nio.file.Paths
import java.time.Duration

const val APTOS_LOCAL_PATH = "/home/mkurnikov/bin/aptos"
const val SUI_LOCAL_PATH = "/home/mkurnikov/bin/sui"

@ExtendWith(RemoteRobotExtension::class)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class WelcomeFrameTest {
    init {
        StepsLogger.init()
    }

    @TempDir
    lateinit var tempFolder: File

    @Test
    @Order(1)
    fun `check new project validation`(remoteRobot: RemoteRobot) = with(remoteRobot) {
        welcomeFrame {
            selectNewProjectType("Move")
        }

        // check radio buttons behaviour
        welcomeFrame {
            moveSettingsPanel {
                aptosRadioButton.select()

                assert(bundledAptosUnsupportedComment == null)

                bundledRadioButton.select()
                assert(bundledRadioButton.isSelected())
                assert(!localPathTextField.isEnabled) { "Local path should be disabled if Bundled is selected" }

                waitFor(interval = Duration.ofSeconds(1)) { versionLabel.value.contains("aptos") }
                assert(validationLabel == null)

                localRadioButton.select()
                assert(localRadioButton.isSelected())
                assert(localPathTextField.isEnabled) { "Local path should be enabled if Local is selected" }

                waitFor(interval = Duration.ofSeconds(1)) { versionLabel.value.contains("N/A") }
                assert(validationLabel?.value == "Invalid path to Aptos executable")
            }
        }

        welcomeFrame {
            moveSettingsPanel {
                suiRadioButton.select()

                tryWithDelay { versionLabel.value.contains("N/A") }
                tryWithDelay { validationLabel?.value == "Invalid path to Sui executable" }
            }
        }

        welcomeFrame {
            cancelButton.click()
        }
    }

    @Test
    @Order(2)
    fun `create new aptos project with bundled cli`(remoteRobot: RemoteRobot) = with(remoteRobot) {
        welcomeFrame {
            selectNewProjectType("Move")
        }

        val projectName = "aptos_project"
        val projectPath = Paths.get(tempFolder.canonicalPath, projectName).toCanonicalPath()

        welcomeFrame {
            moveSettingsPanel {
                aptosRadioButton.select()
                bundledRadioButton.select()

                assert(projectLocationTextField.isEnabled)
                projectLocationTextField.text = projectPath
            }
            createButton.click()
        }

        ideaFrame {
            assert(textEditor().editor.text.contains("[dependencies.AptosFramework]"))

            openSettingsDialog()
            settingsDialog {
                openMoveSettings()
                configurableEditor {
                    moveSettingsPanel {
                        assert(aptosRadioButton.isSelected())
                        assert(!suiRadioButton.isSelected())

                        assert(bundledRadioButton.isSelected())
                        assert(!localRadioButton.isSelected())
                        assert(!localPathTextField.isEnabled)
                    }
                }
                button("Cancel").click()
            }
        }

        ideaFrame {
            closeProject()
        }
        welcomeFrame {
            removeProjectFromRecents(projectName)
        }
    }

    @Test
    @Order(2)
    fun `create new aptos project with local cli`(remoteRobot: RemoteRobot) = with(remoteRobot) {
        welcomeFrame {
            selectNewProjectType("Move")
        }

        val projectName = "aptos_project"
        val projectPath = Paths.get(tempFolder.canonicalPath, projectName).toCanonicalPath()

        welcomeFrame {
            moveSettingsPanel {
                aptosRadioButton.select()

                localRadioButton.select()
                localPathTextField.text = APTOS_LOCAL_PATH

                projectLocationTextField.text = projectPath
            }
            createButton.click()
        }

        ideaFrame {
            assert(textEditor().editor.text.contains("[dependencies.AptosFramework]"))

            openSettingsDialog()
            settingsDialog {
                openMoveSettings()
                configurableEditor {
                    moveSettingsPanel {
                        assert(aptosRadioButton.isSelected())
                        assert(!suiRadioButton.isSelected())

                        assert(!bundledRadioButton.isSelected())
                        assert(localRadioButton.isSelected())

                        assert(localPathTextField.text == APTOS_LOCAL_PATH)
                    }
                }
                button("Cancel").click()
            }

        }

        ideaFrame {
            closeProject()
        }
        welcomeFrame {
            removeProjectFromRecents(projectName)
        }
    }

    @Test
    @Order(3)
    fun `create new sui project`(remoteRobot: RemoteRobot) = with(remoteRobot) {
        welcomeFrame {
            selectNewProjectType("Move")
        }

        val projectName = "sui_project"
        val projectPath = Paths.get(tempFolder.canonicalPath, projectName).toCanonicalPath()

        welcomeFrame {
            moveSettingsPanel {
                suiRadioButton.select()
                localPathTextField.text = SUI_LOCAL_PATH
                projectLocationTextField.text = projectPath
            }
            createButton.click()
        }

        ideaFrame {
            assert(textEditor().editor.text.contains("https://github.com/MystenLabs/sui.git"))

            openSettingsDialog()
            settingsDialog {
                openMoveSettings()
                configurableEditor {
                    moveSettingsPanel {
                        assert(!aptosRadioButton.isSelected())
                        assert(suiRadioButton.isSelected())

                        assert(localPathTextField.text == SUI_LOCAL_PATH)
                    }
                }
                button("Cancel").click()
            }
        }

        ideaFrame {
            closeProject()
        }
        welcomeFrame {
            removeProjectFromRecents(projectName)
        }
    }
}