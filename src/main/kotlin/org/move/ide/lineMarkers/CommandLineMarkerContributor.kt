package org.move.ide.lineMarkers

import com.intellij.execution.lineMarker.ExecutorAction
import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.psi.PsiElement
import org.move.cli.runConfigurations.producers.endless.EndlessTestCommandConfigurationProducer
import org.move.ide.MoveIcons
import org.move.lang.MvElementTypes.IDENTIFIER
import org.move.lang.core.psi.MvFunction
import org.move.lang.core.psi.MvModule
import org.move.lang.core.psi.MvNameIdentifierOwner
import org.move.lang.core.psi.ext.elementType
import org.move.lang.core.psi.ext.isTest
import org.move.lang.core.psi.ext.queryAttributes

class CommandLineMarkerContributor: RunLineMarkerContributor() {
    override fun getInfo(element: PsiElement): Info? {
        if (element.elementType != IDENTIFIER) return null

        val parent = element.parent
        if (parent !is MvNameIdentifierOwner || element != parent.nameIdentifier) return null

        if (parent is MvFunction) {
            when {
                parent.queryAttributes.isTest -> {
                    val config =
                        EndlessTestCommandConfigurationProducer().configFromLocation(parent, climbUp = false)
                    if (config != null) {
                        return Info(
                            MoveIcons.RUN_TEST_ITEM,
                            contextActions(),
                        ) { config.configurationName }
                    }
                }
            }
        }
        if (parent is MvModule) {
            val testConfig =
                EndlessTestCommandConfigurationProducer().configFromLocation(parent, climbUp = false)
            if (testConfig != null) {
                return Info(
                    MoveIcons.RUN_ALL_TESTS_IN_ITEM,
                    contextActions(),
                ) { testConfig.configurationName }
            }
        }
        return null
    }
}

private fun contextActions(): Array<AnAction> {
    return ExecutorAction.getActions(0).toList()
        .toTypedArray()
}
