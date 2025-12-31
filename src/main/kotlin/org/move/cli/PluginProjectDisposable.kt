package org.move.cli

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service

@Service(Service.Level.APP)
class PluginApplicationDisposable: Disposable {
    override fun dispose() {
    }

    override fun toString(): String = "ENDLESS_PLUGIN_DISPOSABLE"
}