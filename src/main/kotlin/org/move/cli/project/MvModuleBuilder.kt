package org.move.cli.project

import com.intellij.ide.util.projectWizard.ModuleBuilder
import com.intellij.openapi.module.ModuleType

class MvModuleBuilder : ModuleBuilder() {
    override fun getModuleType(): ModuleType<*> = MvModuleType.INSTANCE
}
