// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package org.move.ui.fixtures

import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.data.RemoteComponent
import com.intellij.remoterobot.fixtures.*
import com.intellij.remoterobot.search.locators.Locator
import com.intellij.remoterobot.search.locators.byXpath
import com.intellij.remoterobot.stepsProcessing.step
import com.intellij.remoterobot.utils.waitFor
import java.time.Duration

fun RemoteRobot.ideaFrame(function: IdeaFrame.() -> Unit) {
    find<IdeaFrame>(timeout = Duration.ofSeconds(10)).apply(function)
}

fun RemoteRobot.executeCmd(cmd: String, workDir: String): String =
    this.callJs(
        """
            importClass(java.lang.StringBuilder)
            importPackage(java.io)
            
            let result = null;
            const builder = new StringBuilder();
            const pBuilder = new ProcessBuilder(${
            cmd.split(" ").joinToString(separator = "\", \"", prefix = "\"", postfix = "\"")
        })
                .directory(File("$workDir"))
                .redirectErrorStream(true);
            let p;
            try {
                let s;
                p = pBuilder.start();
                const br = new BufferedReader(new InputStreamReader(p.getInputStream()));
                while ((s = br.readLine()) != null) {
                    builder.append(s + "\n")
                }
                p.waitFor();
                result =  builder.toString();
            } catch (e) {
                result = e.getMessage().toString()
            } finally {
                if (p) { p.destroy(); }
            }
            result;
        """
    )


fun ContainerFixture.findIsVisible(locator: Locator): Boolean =
    this.findAll<ComponentFixture>(locator).isNotEmpty()

fun ContainerFixture.findIsNotVisible(locator: Locator): Boolean = !findIsVisible(locator)

@FixtureName("Idea frame")
@DefaultXpath("IdeFrameImpl type", "//div[@class='IdeFrameImpl']")
class IdeaFrame(
    remoteRobot: RemoteRobot,
    remoteComponent: RemoteComponent
):
    CommonContainerFixture(remoteRobot, remoteComponent) {

    val projectViewTree
        get() = find<ContainerFixture>(byXpath("ProjectViewTree", "//div[@class='ProjectViewTree']"))

    val projectName
        get() = step("Get project name") { return@step callJs<String>("component.getProject().getName()") }

    val menuBar: JMenuBarFixture
        get() = step("Menu...") {
            return@step remoteRobot.find(
                JMenuBarFixture::class.java, JMenuBarFixture.byType(), Duration.ofSeconds(5)
            )
        }

    val inlineProgressPanel get() = find<CommonContainerFixture>(byXpath("//div[@class='InlineProgressPanel']"))

//    private fun openSettingsDialog() {
//        if (!remoteRobot.isMac()) {
//            waitFor {
//                findAll<ComponentFixture>(
//                    Locators.byTypeAndProperties(JMenu::class.java, Locators.XpathProperty.ACCESSIBLE_NAME to "File")
//                )
//                    .isNotEmpty()
//            }
//        }
//        menuBar.select("File", "Settings...")
//        waitFor {
//            findAll<DialogFixture>(DialogFixture.byTitle("Settings")).isNotEmpty()
//        }
//    }

    fun settingsDialog(function: SettingsDialogFixture.() -> Unit) =
        find<SettingsDialogFixture>(
            locator = DialogFixture.byTitle("Settings"),
            timeout = Duration.ofSeconds(20)
        ).apply(function)
//        dialog("Settings", function = function)

    fun SettingsDialogFixture.selectMoveSettings() {
        val settingsTreeView = find<ComponentFixture>(byXpath("//div[@class='SettingsTreeView']"))
        settingsTreeView.findText("Languages & Frameworks").click()
        configurableEditor {
            find<ActionLinkFixture>(byXpath("//div[@text='Move Language']")).click()
        }
    }

    fun moveSettings(function: MoveSettingsPanelFixture.() -> Unit) {
        // show settings dialog
        remoteRobot.commonSteps.invokeAction("ShowSettings")
        waitFor {
            findAll<DialogFixture>(DialogFixture.byTitle("Settings")).isNotEmpty()
        }
        remoteRobot.commonSteps.waitMs(300)

        settingsDialog {
            selectMoveSettings()
            configurableEditor {
                moveSettingsPanel(function = function)
            }
            doOKAction()
        }
    }

    @JvmOverloads
    fun dumbAware(timeout: Duration = Duration.ofMinutes(5), function: () -> Unit) {
        step("Wait for smart mode") {
            waitFor(duration = timeout, interval = Duration.ofSeconds(5)) {
                runCatching { isDumbMode().not() }.getOrDefault(false)
            }
            function()
            step("..wait for smart mode again") {
                waitFor(duration = timeout, interval = Duration.ofSeconds(5)) {
                    isDumbMode().not()
                }
            }
        }
    }

    fun isDumbMode(): Boolean {
        return callJs(
            """
            const frameHelper = com.intellij.openapi.wm.impl.ProjectFrameHelper.getFrameHelper(component)
            if (frameHelper) {
                const project = frameHelper.getProject()
                project ? com.intellij.openapi.project.DumbService.isDumb(project) : true
            } else { 
                true 
            }
        """, true
        )
    }

    fun openFile(path: String) {
        runJs(
            """
            importPackage(com.intellij.openapi.fileEditor)
            importPackage(com.intellij.openapi.vfs)
            importPackage(com.intellij.openapi.wm.impl)
            importClass(com.intellij.openapi.application.ApplicationManager)
            
            const path = '$path'
            const frameHelper = ProjectFrameHelper.getFrameHelper(component)
            if (frameHelper) {
                const project = frameHelper.getProject()
                const projectPath = project.getBasePath()
                const file = LocalFileSystem.getInstance().findFileByPath(projectPath + '/' + path)
                const openFileFunction = new Runnable({
                    run: function() {
                        FileEditorManager.getInstance(project).openTextEditor(
                            new OpenFileDescriptor(
                                project,
                                file
                            ), true
                        )
                    }
                })
                ApplicationManager.getApplication().invokeLater(openFileFunction)
            }
        """, true
        )
    }

    fun closeAllEditorTabs() = remoteRobot.commonSteps.invokeAction("CloseAllEditors")
}