package org.move.ide.lineMarkers

import com.intellij.execution.lineMarker.ExecutorAction
import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.psi.PsiElement
import org.move.cli.runconfig.producers.PublishModuleRunConfigurationProducer
import org.move.ide.MoveIcons
import org.move.lang.MvElementTypes.IDENTIFIER
import org.move.lang.core.psi.MvModule
import org.move.lang.core.psi.MvNameIdentifierOwner
import org.move.lang.core.psi.ext.elementType
import org.move.lang.core.psi.ext.hasTestFunctions

class PublishRunLineMarkerContributor : RunLineMarkerContributor() {
    override fun getInfo(element: PsiElement): Info? {
        if (element.elementType != IDENTIFIER) return null
        val parent = element.parent
        if (parent !is MvNameIdentifierOwner || element != parent.nameIdentifier) return null
        if (parent is MvModule && parent.hasTestFunctions()) return null

        val configurationFromContext =
            PublishModuleRunConfigurationProducer.fromLocation(parent, false) ?: return null
        return Info(
            MoveIcons.PUBLISH,
            { configurationFromContext.configurationName },
            *ExecutorAction.getActions(1)
        )
    }
}
