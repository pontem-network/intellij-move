package org.move.project.configurable

import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.util.Disposer
import com.intellij.ui.layout.panel
import org.move.project.settings.ui.MoveProjectSettingsPanel

class MoveProjectConfigurable(project: Project) : BoundConfigurable("Move"),
                                                  Configurable.NoScroll {
    private val moveProjectSettings = MoveProjectSettingsPanel()

    override fun createPanel(): DialogPanel = panel {
        moveProjectSettings.attachTo(this)
    }

    override fun disposeUIResources() {
        super.disposeUIResources()
        Disposer.dispose(moveProjectSettings)
    }
}