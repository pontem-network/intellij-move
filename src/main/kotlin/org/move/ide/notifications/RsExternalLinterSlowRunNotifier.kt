package org.move.ide.notifications

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.util.registry.RegistryValue
import com.intellij.openapi.util.text.HtmlChunk
import com.intellij.openapi.wm.WindowManager
import org.move.cli.externalLinter.RsExternalLinterWidget
import org.move.cli.externalLinter.externalLinterSettings
import java.util.*
import javax.swing.event.HyperlinkEvent

@Service(Service.Level.PROJECT)
class RsExternalLinterSlowRunNotifier(val project: Project) {
    private val maxDuration: Int get() = LINTER_MAX_DURATION.asInteger()
    private val prevDurations: Queue<Long> = ArrayDeque()

    fun reportDuration(duration: Long) {
        prevDurations.add(duration)
        while (prevDurations.size > MAX_QUEUE_SIZE) {
            prevDurations.remove()
        }

        if (PropertiesComponent.getInstance().getBoolean(DO_NOT_SHOW_KEY, false)) return

        val minPrevDuration = prevDurations.minOrNull() ?: 0
        if (prevDurations.size == MAX_QUEUE_SIZE && minPrevDuration > maxDuration) {
            val statusBar = WindowManager.getInstance().getStatusBar(project) ?: return
            val widget = statusBar.getWidget(RsExternalLinterWidget.ID) as? RsExternalLinterWidget ?: return
            val content = "Low performance due to Move external linter${HtmlChunk.br()}" +
                    "${HtmlChunk.link("disable","Disable")}" +
                    "&nbsp;&nbsp;&nbsp;&nbsp;" +
                    "${HtmlChunk.link("dont-show-again", "Don't show again")}"
                widget.showBalloon(content, MessageType.WARNING, project) { e ->
                    if (e?.eventType == HyperlinkEvent.EventType.ACTIVATED) {
                        when (e.description) {
                            "disable" -> project.externalLinterSettings.modify { it.runOnTheFly = false }
                            "dont-show-again" -> PropertiesComponent.getInstance()
                                .setValue(DO_NOT_SHOW_KEY, true, false)
                        }
                    }
                }
                }
    }

    companion object {
        private const val MAX_QUEUE_SIZE: Int = 5
        private const val DO_NOT_SHOW_KEY: String = "org.move.external.linter.slow.run.do.not.show"
        private val LINTER_MAX_DURATION: RegistryValue = Registry.get("org.move.external.linter.max.duration")
    }
}
