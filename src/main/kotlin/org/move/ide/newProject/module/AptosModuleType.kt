package org.move.ide.newProject.module

import com.intellij.openapi.module.ModuleType
import com.intellij.openapi.module.ModuleTypeManager
import org.move.ide.MoveIcons
import javax.swing.Icon

class AptosModuleType: ModuleType<AptosModuleBuilder>(ID) {
    override fun getNodeIcon(isOpened: Boolean): Icon = MoveIcons.MOVE_LOGO

    override fun createModuleBuilder(): AptosModuleBuilder = AptosModuleBuilder()

    override fun getName(): String = "Aptos"
    override fun getDescription(): String = "Aptos module"

    object Util {
        val INSTANCE: AptosModuleType by lazy {
            ModuleTypeManager.getInstance().findByID(ID) as AptosModuleType
        }
    }

    companion object {
        const val ID = "MOVE_MODULE"
    }
}
