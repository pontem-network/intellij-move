package org.move.ide.notifications

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.util.Disposer
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

    private val maxExecutionTime: Int get() = LINTER_MAX_EXECUTION_TIME.asInteger()
    private val prevExecutionTimes: Queue<Long> = ArrayDeque()

    fun reportExecutionTime(executionTime: Long) {
        prevExecutionTimes.add(executionTime)
        while (prevExecutionTimes.size > MAX_QUEUE_SIZE) {
            prevExecutionTimes.remove()
        }

        if (PropertiesComponent.getInstance().getBoolean(DO_NOT_SHOW_KEY, false)) return

        val minPrevExecutionTime = prevExecutionTimes.minOrNull() ?: 0
        // only one of the execution needs to be less than `maxExecutionTime` to be fast enough,
        // we don't want to disable lint execution every time there's one-time lag
        if (prevExecutionTimes.size == MAX_QUEUE_SIZE && minPrevExecutionTime > maxExecutionTime) {
            val statusBar = WindowManager.getInstance().getStatusBar(project) ?: return
            val widget = statusBar.getWidget(RsExternalLinterWidget.ID) as? RsExternalLinterWidget ?: return
            val content = "Low performance due to Move external linter${HtmlChunk.br()}" +
                    "${HtmlChunk.link("disable", "Disable")}" +
                    "&nbsp;&nbsp;&nbsp;&nbsp;" +
                    "${HtmlChunk.link("dont-show-again", "Don't show again")}"
            widget.showBalloon(content, MessageType.WARNING, balloonsDisposable) { e ->
                if (e?.eventType == HyperlinkEvent.EventType.ACTIVATED) {
                    when (e.description) {
                        "disable" -> project.externalLinterSettings.modify { it.runOnTheFly = false }
                        "dont-show-again" -> PropertiesComponent.getInstance()
                                .setValue(DO_NOT_SHOW_KEY, true, false)
                    }
                    // balloons can stack on one another sometimes, we need to remove them all if any action is called
                    disposeAllBalloons()
                }
            }
        }
    }

    private var balloonsDisposable = Disposer.newDisposable()

    private fun disposeAllBalloons() {
        Disposer.dispose(balloonsDisposable)
        this.balloonsDisposable = Disposer.newDisposable()
    }

    companion object {
        private const val MAX_QUEUE_SIZE: Int = 5
        private const val DO_NOT_SHOW_KEY: String = "org.move.external.linter.slow.run.do.not.show"
        private val LINTER_MAX_EXECUTION_TIME: RegistryValue = Registry.get("org.move.external.linter.max.duration")
    }
}
