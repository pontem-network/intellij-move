package org.move.project.configurable

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.util.Disposer
import com.intellij.ui.layout.panel
import org.move.cli.Constants
import org.move.project.settings.ui.MoveProjectSettingsPanel
import java.nio.file.Path

class MoveProjectConfigurable(val project: Project) : BoundConfigurable("Move"),
                                                      Configurable.NoScroll {
    private val moveProjectSettings: MoveProjectSettingsPanel

    init {
        val executablePath = PropertiesComponent.getInstance(project)
                .getValue(Constants.DOVE_EXECUTABLE_PATH_PROPERTY, "")
        moveProjectSettings = MoveProjectSettingsPanel(executablePath)
    }

    override fun createPanel(): DialogPanel = panel {
        moveProjectSettings.attachTo(this)
    }

    override fun disposeUIResources() {
        PropertiesComponent.getInstance(project)
                .setValue(
                    Constants.DOVE_EXECUTABLE_PATH_PROPERTY,
                    moveProjectSettings.getExecutablePath()
                )

        super.disposeUIResources()
        Disposer.dispose(moveProjectSettings)
    }
}

fun Project.pathToDoveExecutable(): String {
    return PropertiesComponent
            .getInstance(this)
            .getValue(Constants.DOVE_EXECUTABLE_PATH_PROPERTY, "")
}