// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package org.move.ui.fixtures

import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.SearchContext
import com.intellij.remoterobot.data.RemoteComponent
import com.intellij.remoterobot.fixtures.*
import com.intellij.remoterobot.search.locators.Locator
import com.intellij.remoterobot.search.locators.byXpath
import com.intellij.remoterobot.stepsProcessing.step
import com.intellij.remoterobot.utils.Locators
import com.intellij.remoterobot.utils.keyboard
import com.intellij.remoterobot.utils.waitFor
import com.intellij.ui.dsl.builder.components.DslLabel
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Duration
import javax.swing.JMenu
import kotlin.math.abs

fun RemoteRobot.welcomeFrame(function: WelcomeFrame.() -> Unit) {
    find(WelcomeFrame::class.java, Duration.ofSeconds(10)).apply(function)
}

fun RemoteRobot.openProject(projectPath: Path) {
    welcomeFrame {
        openProjectAt(projectPath)
    }
//    ideaFrame {
//        find<ComponentFixture>(
//            Locators.byTypeAndProperties(
//                JMenu::class.java,
//                Locators.XpathProperty.ACCESSIBLE_NAME to "File"
//            ),
//            timeout = Duration.ofSeconds(5)
//        )
//    }
}

fun <T: Fixture> SearchContext.findOrNull(type: Class<T>, locator: Locator) =
    findAll(type, locator).firstOrNull()

@FixtureName("Welcome Frame")
@DefaultXpath("type", "//div[@class='FlatWelcomeFrame']")
class WelcomeFrame(
    remoteRobot: RemoteRobot,
    remoteComponent: RemoteComponent
):
    CommonContainerFixture(remoteRobot, remoteComponent) {

    val newProjectButton: JButtonFixture
        get() {
            val cleanStartupButton =
                findOrNull(JButtonFixture::class.java, byXpath("//div[@defaulticon='createNewProjectTab.svg']"))
            if (cleanStartupButton != null) {
                return cleanStartupButton
            }

            return button(
                byXpath("//div[@class='JBOptionButton' and @text='New Project']"),
                timeout = Duration.ofSeconds(2)
            )
        }

    val openProjectButton: JButtonFixture
        get() {
            val cleanStartupButton =
                findOrNull(JButtonFixture::class.java, byXpath("//div[@defaulticon='open.svg']"))
            if (cleanStartupButton != null) {
                return cleanStartupButton
            }

            return button(
                byXpath("//div[@class='JBOptionButton' and @text='Open']"),
                timeout = Duration.ofSeconds(2)
            )
        }


    fun selectNewProjectType(type: String) {
        newProjectButton.click()
        projectTypesList.findText(type).click()
    }

    val validationLabel: JLabelFixture?
        get() =
            findOrNull(JLabelFixture::class.java, byXpath("//div[@defaulticon='lightning.svg']"))

    val projectTypesList
        get() = find(ComponentFixture::class.java, byXpath("//div[@class='JBList']"))

    val projectLocationTextField get() = textFieldWithBrowseButton("Location:")
//        textField(byXpath("//div[@accessiblename='Location:' and @class='TextFieldWithBrowseButton']"))

    val createButton get() = button("Create")
    val cancelButton get() = button("Cancel")

    @Suppress("UnstableApiUsage")
    val bundledAptosUnsupportedComment: ComponentFixture?
        get() {
            val labelText = "is not available for this platform"
            return findOrNull(
                ComponentFixture::class.java,
                Locators.byTypeAndPropertiesContains(
                    DslLabel::class.java,
                    Locators.XpathProperty.TEXT to labelText
                )
            )
        }

    fun openProjectAt(path: Path) {
        openProjectButton.click()
        dialog("Open File or Project") {
            val fileTree = jTree()
            fileTree.collapsePath("/")
            fileTree.clickPath("/")

            val pathTextField = textField(byXpath("//div[@class='BorderlessTextField']"))
            pathTextField.click()

            val absPath = path.toAbsolutePath().toString().drop(1)
            keyboard {
                enterText(absPath, 10)
            }

            val treePath = arrayOf("/") + absPath.split(File.separator).toTypedArray()
            waitFor {
                fileTree.isPathExists(*treePath)
            }
            fileTree.clickPath(*treePath)

            Thread.sleep(300)
            button("OK").click()
        }
    }

    fun openRecentProject(projectName: String) {
        findText(projectName).click()
    }

    fun removeProjectFromRecents(projectName: String) {
        findText(projectName).rightClick()
        jPopupMenu().menuItem("Remove from Recent Projects…").click()
        dialog("Remove Recent Project") {
            button("Remove").click()
        }
    }

    val createNewProjectLink
        get() = actionLink(
            byXpath(
                "New Project",
                "//div[(@class='MainButton' and @text='New Project') or (@accessiblename='New Project' and @class='JButton')]"
            )
        )

    val moreActions
        get() = button(byXpath("More Action", "//div[@accessiblename='More Actions']"))

    val heavyWeightPopup
        get() = remoteRobot.find(ComponentFixture::class.java, byXpath("//div[@class='HeavyWeightWindow']"))
}