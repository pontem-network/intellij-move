package org.move.ui.fixtures

import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.data.RemoteComponent
import com.intellij.remoterobot.fixtures.CommonContainerFixture
import com.intellij.remoterobot.fixtures.ComponentFixture
import com.intellij.remoterobot.fixtures.JLabelFixture
import com.intellij.remoterobot.search.locators.Locator
import com.intellij.remoterobot.stepsProcessing.step
import com.intellij.remoterobot.utils.RelativeLocators
import java.time.Duration

fun CommonContainerFixture.textFieldWithBrowseButton(
    locator: Locator,
    timeout: Duration = Duration.ofSeconds(5)
) = find<TextFieldWithBrowseButtonFixture>(locator, timeout)

fun CommonContainerFixture.textFieldWithBrowseButton(labelText: String): TextFieldWithBrowseButtonFixture {
    val locator = TextFieldWithBrowseButtonFixture.byLabel(jLabel(labelText))
    return textFieldWithBrowseButton(locator)
}


class TextFieldWithBrowseButtonFixture(
    remoteRobot: RemoteRobot,
    remoteComponent: RemoteComponent
):
    ComponentFixture(remoteRobot, remoteComponent) {

    companion object {
//        @JvmStatic
//        fun byType() = Locators.byType(TextFieldWithBrowseButton::class.java)

        @JvmStatic
        fun byLabel(fixture: JLabelFixture): Locator {
            return RelativeLocators.byLabel<TextFieldWithBrowseButton>(fixture)
        }
    }

    var text: String
        set(value) = step("Set text '$value'") {
            runJs(
                "JTextComponentFixture(robot, component.getTextField())" +
                        ".setText('${value.replace("\\", "\\\\")}')"
            )
        }
        get() = step("Get text") {
            callJs("component.getTextField().getText() || ''", true)
        }

    val isEnabled: Boolean
        get() = step("..is enabled?") {
            callJs("component.isEnabled()", true)
        }
}