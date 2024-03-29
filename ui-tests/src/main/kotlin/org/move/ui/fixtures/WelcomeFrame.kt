// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package org.move.ui.fixtures

import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.SearchContext
import com.intellij.remoterobot.data.RemoteComponent
import com.intellij.remoterobot.fixtures.*
import com.intellij.remoterobot.search.locators.Locator
import com.intellij.remoterobot.search.locators.byXpath
import com.intellij.remoterobot.utils.Locators
import com.intellij.remoterobot.utils.waitFor
import com.intellij.ui.dsl.builder.components.DslLabel
import java.time.Duration

fun RemoteRobot.welcomeFrame(function: WelcomeFrame.() -> Unit) {
    find(WelcomeFrame::class.java, Duration.ofSeconds(10)).apply(function)
}

fun tryWithDelay(delay: Long = 1, condition: () -> Boolean) {
    waitFor(
        duration = Duration.ofSeconds(delay),
        interval = Duration.ofSeconds(delay),
        condition = condition
    )
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

    fun newProjectButton(onStartup: Boolean = true) =
        if (onStartup) {
            // clean IDE
            button(byXpath("//div[@defaulticon='createNewProjectTab.svg']"), Duration.ofSeconds(2))
        } else {
            // projects opened before
            button(byXpath("//div[@class='JBOptionButton' and @text='New Project']"))
        }

    val versionLabel get() = jLabel(byXpath("//div[@class='VersionLabel']"), timeout = Duration.ofSeconds(1))
    val validationLabel: JLabelFixture?
        get() =
            findOrNull(JLabelFixture::class.java, byXpath("//div[@defaulticon='lightning.svg']"))

    val projectTypesList
        get() = find(ComponentFixture::class.java, byXpath("//div[@class='JBList']"))

    val aptosRadioButton get() = radioButton("Aptos")
    val suiRadioButton get() = radioButton("Sui")

    val bundledRadioButton get() = radioButton("Bundled")
    val localRadioButton get() = radioButton("Local")

    val localPathTextField get() =
        textFieldWithBrowseButton(byXpath("//div[@class='DialogPanel']//div[@class='TextFieldWithBrowseButton']"))

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