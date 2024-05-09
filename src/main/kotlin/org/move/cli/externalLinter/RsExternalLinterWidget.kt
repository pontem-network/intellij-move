/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.move.cli.externalLinter

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.CustomStatusBarWidget
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.StatusBarWidgetFactory
import com.intellij.openapi.wm.impl.status.TextPanel
import com.intellij.openapi.wm.impl.status.widget.StatusBarWidgetsManager
import com.intellij.ui.ClickListener
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import org.move.cli.MoveProject
import org.move.cli.MoveProjectsService
import org.move.cli.runConfigurations.hasMoveProject
import org.move.cli.settings.MvProjectSettingsServiceBase.*
import org.move.cli.settings.MvProjectSettingsServiceBase.Companion.MOVE_SETTINGS_TOPIC
import org.move.ide.MoveIcons
import org.move.ide.notifications.RsExternalLinterTooltipService
import org.move.openapiext.showSettingsDialog
import java.awt.event.MouseEvent
import javax.swing.JComponent

class RsExternalLinterWidgetFactory: StatusBarWidgetFactory {
    override fun getId(): String = RsExternalLinterWidget.ID
    override fun getDisplayName(): String = "Move External Linter"
    override fun isAvailable(project: Project): Boolean = project.hasMoveProject
    override fun createWidget(project: Project): StatusBarWidget = RsExternalLinterWidget(project)
    override fun disposeWidget(widget: StatusBarWidget) = Disposer.dispose(widget)
    override fun canBeEnabledOn(statusBar: StatusBar): Boolean = true
}

class RsExternalLinterWidgetUpdater(private val project: Project): MoveProjectsService.MoveProjectsListener {
    override fun moveProjectsUpdated(service: MoveProjectsService, projects: Collection<MoveProject>) {
        val manager = project.service<StatusBarWidgetsManager>()
        manager.updateWidget(RsExternalLinterWidgetFactory::class.java)
    }
}

class RsExternalLinterWidget(private val project: Project): TextPanel.WithIconAndArrows(),
                                                            CustomStatusBarWidget {
    private var statusBar: StatusBar? = null

    private val linter: ExternalLinter get() = project.externalLinterSettings.tool
    private val turnedOn: Boolean get() = project.externalLinterSettings.runOnTheFly

    var inProgress: Boolean = false
        set(value) {
            field = value
            update()
        }

    init {
        setTextAlignment(CENTER_ALIGNMENT)
        border = JBUI.CurrentTheme.StatusBar.Widget.border()
    }

    override fun ID(): String = ID

    override fun install(statusBar: StatusBar) {
        this.statusBar = statusBar

        if (!project.isDisposed) {
            object: ClickListener() {
                override fun onClick(event: MouseEvent, clickCount: Int): Boolean {
                    if (!project.isDisposed) {
                        project.showSettingsDialog<MvExternalLinterConfigurable>()
                    }
                    return true
                }
            }.installOn(this, true)

            project.messageBus.connect(this).subscribe(MOVE_SETTINGS_TOPIC, object: MoveSettingsListener {
                override fun <T: MvProjectSettingsBase<T>> settingsChanged(e: SettingsChangedEventBase<T>) {
                    if (e !is MvExternalLinterProjectSettingsService.SettingsChangedEvent) return
                    if (e.isChanged(MvExternalLinterProjectSettingsService.MvExternalLinterProjectSettings::tool) ||
                        e.isChanged(MvExternalLinterProjectSettingsService.MvExternalLinterProjectSettings::runOnTheFly)
                    ) {
                        update()
                    }
                }
            })

            project.service<RsExternalLinterTooltipService>().showTooltip(this)
        }

        update()
        statusBar.updateWidget(ID())
    }

    override fun dispose() {
        statusBar = null
        UIUtil.dispose(this)
    }

    override fun getComponent(): JComponent = this

    private fun update() {
        if (project.isDisposed) return
        UIUtil.invokeLaterIfNeeded {
            if (project.isDisposed) return@invokeLaterIfNeeded
            text = linter.title
            val status = if (turnedOn) "ON" else "OFF"
            toolTipText =
                "${linter.title} ${if (inProgress) "is in progress" else "on the fly analysis is turned $status"}"
            icon = when {
                !turnedOn -> MoveIcons.GEAR_OFF
                inProgress -> MoveIcons.GEAR_ANIMATED
                else -> MoveIcons.GEAR
            }
            repaint()
        }
    }

    companion object {
        const val ID: String = "moveExternalLinterWidget"
    }
}
