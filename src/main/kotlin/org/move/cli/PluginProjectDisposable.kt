package org.move.cli

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service

@Service(Service.Level.PROJECT)
class PluginProjectDisposable : Disposable {
    override fun dispose() {
    }
}

@Service(Service.Level.APP)
class PluginApplicationDisposable : Disposable {
    override fun dispose() {
    }
}