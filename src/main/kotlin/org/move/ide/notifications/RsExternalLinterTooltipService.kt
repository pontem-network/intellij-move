package org.move.ide.notifications

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.ui.GotItTooltip
import org.move.cli.externalLinter.ExternalLinter
import org.move.cli.externalLinter.MvExternalLinterConfigurable
import org.move.cli.externalLinter.externalLinterSettings
import org.move.openapiext.showSettingsDialog
import javax.swing.JComponent

@Service(Service.Level.PROJECT)
class RsExternalLinterTooltipService(private val project: Project): Disposable {
    private val linter: ExternalLinter get() = project.externalLinterSettings.tool
    private val turnedOn: Boolean get() = project.externalLinterSettings.runOnTheFly

    fun showTooltip(component: JComponent) {
        val tooltip = createTooltip(turnedOn)
        tooltip.show(component, GotItTooltip.TOP_MIDDLE)
    }

    override fun dispose() {}

    private fun createTooltip(turnedOn: Boolean): GotItTooltip {
        val headerText = "${linter.title} on the fly analysis is turned ${if (turnedOn) "ON" else "OFF"}"
        val text =
            "The analysis shows all problems reported by ${linter.title}, but consumes more system resources. " +
                    "When turned off, only the limited set of problems supported by IntelliJ Move engine are shown."
        return GotItTooltip("move.linter.on-the-fly.got.it", text, this)
            .withHeader(headerText)
            .withLink("Configure...") {
                project.showSettingsDialog<MvExternalLinterConfigurable>()
            }
    }
}
