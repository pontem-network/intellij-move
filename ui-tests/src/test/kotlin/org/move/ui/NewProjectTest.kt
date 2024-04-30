package org.move.ui

import com.intellij.openapi.util.io.toCanonicalPath
import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.fixtures.ContainerFixture
import com.intellij.remoterobot.search.locators.byXpath
import com.intellij.remoterobot.utils.waitFor
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.io.TempDir
import org.move.ui.fixtures.*
import org.move.ui.utils.RemoteRobotExtension
import org.move.ui.utils.StepsLogger
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Duration

const val APTOS_LOCAL_PATH = "/home/mkurnikov/bin/aptos"
const val SUI_LOCAL_PATH = "/home/mkurnikov/bin/sui"

class NewProjectTest: UiTestBase() {
    @Test
    fun `new project validation`(robot: RemoteRobot) = with(robot) {
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
                localPathTextField.text = ""

                waitFor { versionLabel.value.contains("N/A") }
                waitFor { validationLabel?.value == "Invalid path to Aptos executable" }
            }
        }

        welcomeFrame {
            moveSettingsPanel {
                suiRadioButton.select()

                localPathTextField.text = ""

                waitFor { versionLabel.value.contains("N/A") }
                waitFor { validationLabel?.value == "Invalid path to Sui executable" }
            }
        }

        welcomeFrame {
            cancelButton.click()
        }
    }

    @Test
    fun `create new aptos project with bundled cli`(robot: RemoteRobot) = with(robot) {
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

            moveSettings {
                assert(aptosRadioButton.isSelected())
                assert(!suiRadioButton.isSelected())

                assert(bundledRadioButton.isSelected())
                assert(!localRadioButton.isSelected())
                assert(!localPathTextField.isEnabled)
            }
        }
    }

    @Test
    fun `create new aptos project with local cli`(robot: RemoteRobot) = with(robot) {
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

            moveSettings {
                assert(aptosRadioButton.isSelected())
                assert(!suiRadioButton.isSelected())

                assert(!bundledRadioButton.isSelected())
                assert(localRadioButton.isSelected())

                assert(localPathTextField.text == APTOS_LOCAL_PATH)
            }
        }
    }

    @Test
    fun `create new sui project`(robot: RemoteRobot) = with(robot) {
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

            moveSettings {
                assert(!aptosRadioButton.isSelected())
                assert(suiRadioButton.isSelected())
                assert(localPathTextField.text == SUI_LOCAL_PATH)
            }
        }
    }

    @Test
    fun `import existing aptos package`(robot: RemoteRobot) = with(robot) {
        copyExamplePackageToTempFolder("aptos_package")

        val tempPackagePath = tempFolder.toPath().resolve("aptos_package")
        openOrImportProject(tempPackagePath)

        ideaFrame {
            val textEditor = textEditor()
            assert(textEditor.editor.filePath == tempPackagePath.resolve("Move.toml").toString())
        }

        ideaFrame {
            moveSettings {
                assert(aptosRadioButton.isSelected())
                assert(!suiRadioButton.isSelected())
            }
        }
    }

    @Test
    fun `import existing sui package`(robot: RemoteRobot) = with(robot) {
        copyExamplePackageToTempFolder("sui_package")

        val projectPath = tempFolder.toPath().resolve("sui_package")
        openOrImportProject(projectPath)

        ideaFrame {
            val textEditor = textEditor()
            assert(textEditor.editor.filePath == projectPath.resolve("Move.toml").toString())
        }

        ideaFrame {
            moveSettings {
                assert(!aptosRadioButton.isSelected())
                assert(suiRadioButton.isSelected())
            }
        }
    }

    @Test
    fun `explicit sui blockchain setting should retain even if wrong`(robot: RemoteRobot) = with(robot) {
        copyExamplePackageToTempFolder("aptos_package")

        // opens as Aptos package
        val projectPath = tempFolder.toPath().resolve("aptos_package")
        openOrImportProject(projectPath)

        // mark project as Sui
        ideaFrame {
            moveSettings {
                suiRadioButton.select()
            }
        }

        // reopen project to see that no ProjectActivity or OpenProcessor changed the setting
        closeProject()
        openOrImportProject(projectPath)

        ideaFrame {
            moveSettings {
                assert(!aptosRadioButton.isSelected())
                assert(suiRadioButton.isSelected())
            }
        }
    }

    @Test
    fun `no move toml opened after reopen from new project`(robot: RemoteRobot) = with(robot) {
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

        ideaFrame { closeAllEditorTabs() }
        closeProject()
        openOrImportProject(projectPath)

        ideaFrame {
            assert(textEditors().isEmpty())
        }
    }

    @Test
    fun `no move toml opened after reopen from existing project import`(robot: RemoteRobot) = with(robot) {
        copyExamplePackageToTempFolder("aptos_package")

        // opens as Aptos package
        val projectPath = tempFolder.toPath().resolve("aptos_package")
        openOrImportProject(projectPath)

        ideaFrame { closeAllEditorTabs() }
        closeProject()
        openOrImportProject(projectPath)

        ideaFrame {
            assert(textEditors().isEmpty())
        }
    }

    // TODO
//    @Test
//    fun `no default compile configuration should be created in pycharm`(robot: RemoteRobot) = with(robot) {
//
//
//    }
}