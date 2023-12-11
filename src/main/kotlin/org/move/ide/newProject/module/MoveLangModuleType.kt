package org.move.ide.newProject.module

import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.ide.wizard.*
import com.intellij.ide.wizard.NewProjectWizardChainStep.Companion.nextStep
import com.intellij.openapi.module.ModuleType
import com.intellij.openapi.module.ModuleTypeManager
import com.intellij.openapi.observable.properties.PropertyGraph
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.ui.dsl.builder.Panel
import org.move.ide.MoveIcons
import javax.swing.Icon

class MoveLangProjectWizard: GeneratorNewProjectWizard {
    override val id: String get() = "org.move.Move2"
    override val name: String get() = "Move2"
    override val icon: Icon
        get() = MoveIcons.MOVE_LOGO

    override fun createStep(context: WizardContext): NewProjectWizardStep {
        return MoveLangProjectWizardStep(context)
    }
}

class MoveLangProjectWizardStep(override val context: WizardContext): NewProjectWizardStep {
    override val data = UserDataHolderBase()

    override val propertyGraph = PropertyGraph("New project wizard")

    override var keywords = NewProjectWizardStep.Keywords()

    override fun setupUI(builder: Panel) {
        builder.apply {
            row {
                comment("hello, world")
            }
        }
    }
}

class MoveLangProjectBuilder: GeneratorNewProjectWizardBuilderAdapter(MoveLangProjectWizard())

class MoveLangModuleType: ModuleType<MoveLangModuleBuilder>(ID) {
    override fun getName(): String = "Move"
    override fun getDescription(): String = "Move module"
    override fun getNodeIcon(isOpened: Boolean): Icon = MoveIcons.MOVE_LOGO

    override fun createModuleBuilder(): MoveLangModuleBuilder = MoveLangModuleBuilder()

    object Util {
        val INSTANCE: MoveLangModuleType by lazy {
            ModuleTypeManager.getInstance().findByID(ID) as MoveLangModuleType
        }
    }

    companion object {
        const val ID = "MOVE_MODULE"
    }
}
