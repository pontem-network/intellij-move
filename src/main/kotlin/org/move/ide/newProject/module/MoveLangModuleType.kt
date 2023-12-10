package org.move.ide.newProject.module

import com.intellij.openapi.module.ModuleType
import com.intellij.openapi.module.ModuleTypeManager
import org.move.ide.MoveIcons
import javax.swing.Icon

class AptosModuleType: ModuleType<MoveLangModuleBuilder>(ID) {
    override fun getName(): String = "Move"
    override fun getDescription(): String = "Move module"
    override fun getNodeIcon(isOpened: Boolean): Icon = MoveIcons.MOVE_LOGO

    override fun createModuleBuilder(): MoveLangModuleBuilder = MoveLangModuleBuilder()

    object Util {
        val INSTANCE: AptosModuleType by lazy {
            ModuleTypeManager.getInstance().findByID(ID) as AptosModuleType
        }
    }

    companion object {
        const val ID = "MOVE_MODULE"
    }
}
