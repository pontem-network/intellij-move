package org.move.openapiext

import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessListener
import com.intellij.openapi.util.Key

class CompositeProcessListener(vararg val listeners: ProcessListener): ProcessListener {
    override fun startNotified(event: ProcessEvent) {
        for (listener in listeners) {
            listener.startNotified(event)
        }
    }

    override fun processNotStarted() {
        for (listener in listeners) {
            listener.processNotStarted()
        }
    }

    override fun processWillTerminate(event: ProcessEvent, willBeDestroyed: Boolean) {
        for (listener in listeners) {
            listener.processWillTerminate(event, willBeDestroyed)
        }
    }

    override fun processTerminated(event: ProcessEvent) {
        for (listener in listeners) {
            listener.processTerminated(event)
        }
    }

    override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
        for (listener in listeners) {
            listener.onTextAvailable(event, outputType)
        }
    }
}