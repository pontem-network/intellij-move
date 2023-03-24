package org.move.ide.lineMarkers

import com.intellij.execution.lineMarker.ExecutorAction
import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.psi.PsiElement
import org.move.cli.runConfigurations.producers.PublishCommandConfigurationProducer
import org.move.cli.runConfigurations.producers.TestCommandConfigurationProducer
import org.move.ide.MoveIcons
import org.move.lang.MvElementTypes.IDENTIFIER
import org.move.lang.core.psi.MvFunction
import org.move.lang.core.psi.MvModule
import org.move.lang.core.psi.MvNameIdentifierOwner
import org.move.lang.core.psi.ext.elementType
import org.move.lang.core.psi.ext.isTest

class AptosCommandLineMarkerContributor : RunLineMarkerContributor() {
    override fun getInfo(element: PsiElement): Info? {
        if (element.elementType != IDENTIFIER) return null

        val parent = element.parent
        if (parent !is MvNameIdentifierOwner || element != parent.nameIdentifier) return null

        if (parent is MvFunction) {
            when {
                parent.isTest -> {
                    val config =
                        TestCommandConfigurationProducer.fromLocation(parent, climbUp = false)
                    if (config != null) {
                        return Info(
                            MoveIcons.RUN_TEST_ITEM,
                            { config.configurationName },
                            *contextActions()
                        )
                    }
                }
//                parent.isEntry -> {
//                    val moveProject = parent.moveProject ?: return null
//                    val paramsDialog =
//                        RunTransactionDialog.showAndGetOk(parent, moveProject) ?: return null
//                    val commandLineFromContext = paramsDialog.toAptosCommandLineFromContext()
//                    val runTransactionCommandLine = paramsDialog.toAptosCommandLine() ?: return null
//                    runTransactionCommandLine.run(
//                        moveProject,
//                        paramsDialog.configurationName,
//                        saveConfiguration = true
//                    )
//                }
            }
        }
        if (parent is MvModule) {
            val publishConfig =
                PublishCommandConfigurationProducer.fromLocation(parent, false)
            if (publishConfig != null) {
                return Info(
                    MoveIcons.PUBLISH,
                    { publishConfig.configurationName },
                    *ExecutorAction.getActions(1)
                )
            }
            val testConfig = TestCommandConfigurationProducer.fromLocation(parent, climbUp = false)
            if (testConfig != null) {
                return Info(
                    MoveIcons.RUN_ALL_TESTS_IN_ITEM,
                    { testConfig.configurationName },
                    *contextActions()
                )
            }
        }
        return null
    }
}

private fun contextActions(): Array<AnAction> {
    return ExecutorAction.getActions(0).toList()
//        .filter { it.toString().startsWith("Run context configuration") }
        .toTypedArray()
}
