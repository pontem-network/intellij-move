package org.move.cli.runconfig

import com.intellij.execution.actions.ConfigurationContext
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement

class CompositeRunConfigurationProducer: MoveRunConfigurationProducer() {
    override fun isConfigurationFromContext(
        configuration: MoveRunConfiguration,
        context: ConfigurationContext
    ): Boolean {
        TODO("Not yet implemented")
    }

    override fun setupConfigurationFromContext(
        configuration: MoveRunConfiguration,
        context: ConfigurationContext,
        sourceElement: Ref<PsiElement>
    ): Boolean {
        TODO("Not yet implemented")
    }
}
