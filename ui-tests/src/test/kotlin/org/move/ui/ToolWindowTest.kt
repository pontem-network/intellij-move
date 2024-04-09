package org.move.ui

import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.data.RemoteComponent
import com.intellij.remoterobot.fixtures.ComponentFixture
import com.intellij.remoterobot.fixtures.ContainerFixture
import com.intellij.remoterobot.fixtures.JTreeFixture
import com.intellij.remoterobot.search.locators.byXpath
import org.junit.jupiter.api.Test
import org.move.ui.fixtures.commonSteps
import org.move.ui.fixtures.findIsNotVisible
import org.move.ui.fixtures.ideaFrame
import org.move.ui.fixtures.openOrImportProject

class ProjectsTreeFixture(
    remoteRobot: RemoteRobot,
    remoteComponent: RemoteComponent
):
    JTreeFixture(remoteRobot, remoteComponent)

class ToolWindowTest: UiTestBase() {
    @Test
    fun `aptos tool window not available if move project has no manifest`(robot: RemoteRobot) = with(robot) {
        val projectPath =
            copyExamplePackageToTempFolder("empty_move_package")
        openOrImportProject(projectPath)

        // TODO: change into proper JS call to ToolWindowManager
        ideaFrame {
            val rightStripe =
                find<ContainerFixture>(byXpath("//div[@accessiblename='Right Stripe']"))
            assert(rightStripe.findIsNotVisible(byXpath("//div[@text='Aptos']")))
        }
    }

    @Test
    fun `aptos tool window not available if move project is sui`(robot: RemoteRobot) = with(robot) {
        val projectPath =
            copyExamplePackageToTempFolder("sui_package")
        openOrImportProject(projectPath)

        // TODO: change into proper JS call to ToolWindowManager
        ideaFrame {
            val rightStripe =
                find<ContainerFixture>(byXpath("//div[@accessiblename='Right Stripe']"))
            assert(rightStripe.findIsNotVisible(byXpath("//div[@text='Aptos']")))
        }
    }

    @Test
    fun `aptos tool window for aptos project`(robot: RemoteRobot) = with(robot) {
        val projectPath =
            copyExamplePackageToTempFolder("aptos_package")
        openOrImportProject(projectPath)

        // TODO: change into proper JS call to ToolWindowManager
        ideaFrame {
            val rightStripe =
                find<ContainerFixture>(byXpath("//div[@accessiblename='Right Stripe']"))
            val aptosStripeButton = rightStripe.find<ComponentFixture>(byXpath("//div[@text='Aptos']"))
            aptosStripeButton.click()
            robot.commonSteps.waitMs(500)

            val projectsTree = find<ProjectsTreeFixture>(byXpath("//div[@class='MoveProjectsTree']"))
            projectsTree.expandAll()
            robot.commonSteps.waitMs(500)

            val topLevelsPaths =
                projectsTree.collectExpandedPaths().filter { it.path.size <= 3 }.map { it.path }
            assert(
                topLevelsPaths == listOf(
                    listOf("MyAptosPackage"),
                    listOf("MyAptosPackage", "Dependencies"),
                    listOf("MyAptosPackage", "Dependencies", "MoveStdlib"),
                    listOf("MyAptosPackage", "Dependencies", "AptosStdlib"),
                    listOf("MyAptosPackage", "Dependencies", "AptosFramework"),
                )
            )
        }
    }
}