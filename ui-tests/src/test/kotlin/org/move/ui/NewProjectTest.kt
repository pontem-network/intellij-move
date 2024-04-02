package org.move.ui

import com.intellij.openapi.util.io.toCanonicalPath
import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.fixtures.ComponentFixture
import com.intellij.remoterobot.steps.CommonSteps
import com.intellij.remoterobot.utils.Locators
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
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Duration
import javax.swing.JMenu

const val APTOS_LOCAL_PATH = "/home/mkurnikov/bin/aptos"
const val SUI_LOCAL_PATH = "/home/mkurnikov/bin/sui"

@ExtendWith(RemoteRobotExtension::class)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
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
    @Order(1)
    fun `new project validation`(remoteRobot: RemoteRobot) = with(remoteRobot) {
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
                Thread.sleep(1000)

                waitFor { versionLabel.value.contains("N/A") }
                waitFor { validationLabel?.value == "Invalid path to Aptos executable" }
            }
        }

        welcomeFrame {
            moveSettingsPanel {
                suiRadioButton.select()

                localPathTextField.text = ""
                Thread.sleep(1000)

                waitFor { versionLabel.value.contains("N/A") }
                waitFor { validationLabel?.value == "Invalid path to Sui executable" }
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

            openMoveSettings {
                assert(aptosRadioButton.isSelected())
                assert(!suiRadioButton.isSelected())

                assert(bundledRadioButton.isSelected())
                assert(!localRadioButton.isSelected())
                assert(!localPathTextField.isEnabled)
            }
        }

        CommonSteps(remoteRobot).closeProject()
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

            openMoveSettings {
                assert(aptosRadioButton.isSelected())
                assert(!suiRadioButton.isSelected())

                assert(!bundledRadioButton.isSelected())
                assert(localRadioButton.isSelected())

                assert(localPathTextField.text == APTOS_LOCAL_PATH)
            }
        }

        CommonSteps(remoteRobot).closeProject()
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

            openMoveSettings {
                assert(!aptosRadioButton.isSelected())
                assert(suiRadioButton.isSelected())
                assert(localPathTextField.text == SUI_LOCAL_PATH)
            }
        }

        CommonSteps(remoteRobot).closeProject()
        welcomeFrame {
            removeProjectFromRecents(projectName)
        }
    }

    @Test
    fun `import existing aptos package`(remoteRobot: RemoteRobot) = with(remoteRobot) {
        copyExamplePackageToTempFolder("aptos_package")

        val tempPackagePath = tempFolder.toPath().resolve("aptos_package")
        openProject(tempPackagePath)

        ideaFrame {
            openMoveSettings {
                assert(aptosRadioButton.isSelected())
                assert(!suiRadioButton.isSelected())
            }
        }

        ideaFrame {
            closeProject()
        }
        welcomeFrame {
            removeProjectFromRecents("aptos_package")
        }
    }

    @Test
    fun `import existing sui package`(remoteRobot: RemoteRobot) = with(remoteRobot) {
        copyExamplePackageToTempFolder("sui_package")

        val projectPath = tempFolder.toPath().resolve("sui_package")
        openProject(projectPath)

        ideaFrame {
            openMoveSettings {
                assert(!aptosRadioButton.isSelected())
                assert(suiRadioButton.isSelected())
            }
        }

        CommonSteps(remoteRobot).closeProject()
        welcomeFrame {
            removeProjectFromRecents("sui_package")
        }
    }

    @Test
    @Order(2)
    fun `explicit sui blockchain setting should retain even if wrong`(remoteRobot: RemoteRobot) = with(remoteRobot) {
        copyExamplePackageToTempFolder("aptos_package")

        // opens as Aptos package
        val projectPath = tempFolder.toPath().resolve("aptos_package")
        welcomeFrame {
            openProjectAt(projectPath)
        }

        // mark project as Sui
        ideaFrame {
            openMoveSettings {
                suiRadioButton.select()
            }
        }

        // reopen project to see that no ProjectActivity or OpenProcessor changed the setting
        ideaFrame {
            closeProject()
        }
        welcomeFrame {
            openRecentProject("aptos_package")
        }

        ideaFrame {
            openMoveSettings {
                assert(!aptosRadioButton.isSelected())
                assert(suiRadioButton.isSelected())
            }
        }

        CommonSteps(remoteRobot).closeProject()
        welcomeFrame {
            removeProjectFromRecents("aptos_package")
        }
    }

    // TODO
//    @Test
//    fun `no default compile configuration should be created in pycharm`(remoteRobot: RemoteRobot) = with(remoteRobot) {
//
//
//    }
}