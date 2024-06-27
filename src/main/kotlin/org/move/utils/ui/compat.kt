package org.move.utils.ui

import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopup

object NavigationUtilCompat {
    fun hidePopupIfDumbModeStarts(popup: JBPopup, project: Project) {
        if (!DumbService.isDumb(project)) {
            project.messageBus.connect(popup)
                .subscribe(DumbService.DUMB_MODE, object: DumbService.DumbModeListener {
                    override fun enteredDumbMode() {
                        popup.cancel()
                    }
                })
        }
    }
}
