package org.move.ui

import com.intellij.openapi.util.io.toCanonicalPath
import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.utils.waitFor
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

@ExtendWith(RemoteRobotExtension::class)
class NewProjectTest {
    init {
        StepsLogger.init()
    }

    @TempDir
    lateinit var tempFolder: File

    private fun getResourcesDir(): Path {
        return Paths.get("").toAbsolutePath()
            .resolve("src").resolve("test").resolve("resources")
    }

    private fun getExamplePackagesDir() = getResourcesDir().resolve("example-packages")
    private fun copyExamplePackageToTempFolder(packageName: String) {
        val tempPackagePath = tempFolder.toPath().resolve(packageName)
        getExamplePackagesDir().resolve(packageName).toFile().copyRecursively(tempPackagePath.toFile())
        Thread.sleep(500)
    }

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

            openMoveSettings {
                assert(aptosRadioButton.isSelected())
                assert(!suiRadioButton.isSelected())

                assert(bundledRadioButton.isSelected())
                assert(!localRadioButton.isSelected())
                assert(!localPathTextField.isEnabled)
            }
        }

        closeProject()
        removeLastRecentProject()
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

            openMoveSettings {
                assert(aptosRadioButton.isSelected())
                assert(!suiRadioButton.isSelected())

                assert(!bundledRadioButton.isSelected())
                assert(localRadioButton.isSelected())

                assert(localPathTextField.text == APTOS_LOCAL_PATH)
            }
        }

        closeProject()
        removeLastRecentProject()
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

            openMoveSettings {
                assert(!aptosRadioButton.isSelected())
                assert(suiRadioButton.isSelected())
                assert(localPathTextField.text == SUI_LOCAL_PATH)
            }
        }

        closeProject()
        removeLastRecentProject()
    }

    @Test
    fun `import existing aptos package`(robot: RemoteRobot) = with(robot) {
        copyExamplePackageToTempFolder("aptos_package")

        val tempPackagePath = tempFolder.toPath().resolve("aptos_package")
        openOrImportProject(tempPackagePath)

        ideaFrame {
            openMoveSettings {
                assert(aptosRadioButton.isSelected())
                assert(!suiRadioButton.isSelected())
            }
        }

        closeProject()
        removeLastRecentProject()
    }

    @Test
    fun `import existing sui package`(robot: RemoteRobot) = with(robot) {
        copyExamplePackageToTempFolder("sui_package")

        val projectPath = tempFolder.toPath().resolve("sui_package")
        openOrImportProject(projectPath)

        ideaFrame {
            openMoveSettings {
                assert(!aptosRadioButton.isSelected())
                assert(suiRadioButton.isSelected())
            }
        }

        closeProject()
        removeLastRecentProject()
    }

    @Test
    fun `explicit sui blockchain setting should retain even if wrong`(robot: RemoteRobot) = with(robot) {
        copyExamplePackageToTempFolder("aptos_package")

        // opens as Aptos package
        val projectPath = tempFolder.toPath().resolve("aptos_package")
        openOrImportProject(projectPath)

        // mark project as Sui
        ideaFrame {
            openMoveSettings {
                suiRadioButton.select()
            }
        }

        closeProject()
        // reopen project to see that no ProjectActivity or OpenProcessor changed the setting
        welcomeFrame {
            openRecentProject("aptos_package")
        }

        ideaFrame {
            openMoveSettings {
                assert(!aptosRadioButton.isSelected())
                assert(suiRadioButton.isSelected())
            }
        }

        closeProject()
        removeLastRecentProject()
    }

    // TODO
//    @Test
//    fun `no default compile configuration should be created in pycharm`(robot: RemoteRobot) = with(robot) {
//
//
//    }
}