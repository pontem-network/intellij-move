package org.move.ide.lineMarkers

import com.intellij.execution.lineMarker.ExecutorAction
import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.psi.PsiElement
import org.move.cli.runconfig.producers.TestRunConfigurationProducer
import org.move.ide.MvIcons
import org.move.lang.MvElementTypes.IDENTIFIER
import org.move.lang.core.psi.MvNameIdentifierOwner
import org.move.lang.core.psi.ext.elementType

class TestRunLineMarkerContributor : RunLineMarkerContributor() {
    override fun getInfo(element: PsiElement): Info? {
        if (element.elementType != IDENTIFIER) return null
        val parent = element.parent
        if (parent !is MvNameIdentifierOwner || element != parent.nameIdentifier) return null

        val cmdConfig = TestRunConfigurationProducer.fromLocation(parent, climbUp = false) ?: return null
        return Info(
            MvIcons.TEST,
            { cmdConfig.configurationName },
            *ExecutorAction.getActions(1)
        )
    }
}
