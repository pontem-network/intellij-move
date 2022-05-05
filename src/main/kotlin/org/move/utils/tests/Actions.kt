package org.move.utils.tests

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DataKey
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.testFramework.TestApplicationManager
import com.intellij.testFramework.TestDataProvider
import com.intellij.testFramework.fixtures.CodeInsightTestFixture

fun CodeInsightTestFixture.launchAction(
    actionId: String,
    vararg context: Pair<DataKey<*>, *>,
    shouldBeEnabled: Boolean = true
): Presentation {
    TestApplicationManager.getInstance().setDataProvider(object : TestDataProvider(project) {
        override fun getData(dataId: String): Any? {
            for ((key, value) in context) {
                if (key.`is`(dataId)) return value
            }
            return super.getData(dataId)
        }
    }, testRootDisposable)

    val action = ActionManager.getInstance().getAction(actionId) ?: error("Failed to find action by `$actionId` id")
    val presentation = testAction(action)
    if (shouldBeEnabled) {
        check(presentation.isEnabledAndVisible) { "Failed to run `${action.javaClass.simpleName}` action" }
    } else {
        check(!presentation.isEnabledAndVisible) { "`${action.javaClass.simpleName}` action shouldn't be enabled"}
    }
    return presentation
}
