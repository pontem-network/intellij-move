package org.move.ui.fixtures

import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.data.RemoteComponent
import com.intellij.remoterobot.fixtures.CommonContainerFixture
import com.intellij.remoterobot.fixtures.ContainerFixture
import com.intellij.remoterobot.fixtures.DefaultXpath
import com.intellij.remoterobot.fixtures.FixtureName
import com.intellij.remoterobot.search.locators.byXpath
import com.intellij.remoterobot.stepsProcessing.step
import java.time.Duration

fun ContainerFixture.moveSettingsPanel(
    timeout: Duration = Duration.ofSeconds(20),
    function: MoveSettingsPanelFixture.() -> Unit = {}
): MoveSettingsPanelFixture =
    step("Search for Move settings panel") {
        find(MoveSettingsPanelFixture::class.java, timeout).apply(function)
    }

@FixtureName("MoveSettingsPanel")
@DefaultXpath("DialogPanel type", "//div[@class='DialogPanel']")
class MoveSettingsPanelFixture(
    remoteRobot: RemoteRobot,
    remoteComponent: RemoteComponent
):
    CommonContainerFixture(remoteRobot, remoteComponent) {

    val aptosRadioButton get() = radioButton("Aptos")
    val suiRadioButton get() = radioButton("Sui")

    val bundledRadioButton get() = radioButton("Bundled")
    val localRadioButton get() = radioButton("Local")

    val versionLabel get() = jLabel(byXpath("//div[@class='VersionLabel']"), timeout = Duration.ofSeconds(1))

    val localPathTextField
        get() =
            textFieldWithBrowseButton(byXpath("//div[@class='TextFieldWithBrowseButton']"))
}