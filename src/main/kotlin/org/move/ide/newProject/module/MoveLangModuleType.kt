package org.move.ide.newProject.module

import com.intellij.ide.wizard.*
import com.intellij.openapi.module.ModuleType
import com.intellij.openapi.module.ModuleTypeManager
import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.builder.Panel
import org.move.cli.settings.ChooseAptosCliPanel
import org.move.ide.MoveIcons
import javax.swing.Icon

//class MoveLangProjectWizard: GeneratorNewProjectWizard {
//    override val id: String get() = "org.move.Move2"
//    override val name: String get() = "Move2"
//    override val icon: Icon
//        get() = MoveIcons.MOVE_LOGO
//
//    override fun createStep(context: WizardContext): NewProjectWizardStep {
//        return RootNewProjectWizardStep(context)
//            .nextStep(::MoveLangProjectWizardStep)
//    }
//}

class AptosNewProjectWizard: LanguageNewProjectWizard {
    override val name: String get() = "Aptos"
    override val ordinal: Int get() = 399

    override fun createStep(parent: NewProjectWizardLanguageStep) = Step(parent)

    class Step(private val parent: NewProjectWizardLanguageStep): AbstractNewProjectWizardStep(parent) {

        private val panel = ChooseAptosCliPanel(showDefaultProjectSettingsLink = false)

        override fun setupUI(builder: Panel) {
            with(builder) {
                panel.attachToLayout(this)
            }
        }

        override fun setupProject(project: Project) {
            super.setupProject(project)
        }
    }
}

//class MoveLangProjectWizardStep(parent: NewProjectWizardStep): AbstractNewProjectWizardStep(parent) {
//    override val data = UserDataHolderBase()
//
//    override val propertyGraph = PropertyGraph("New project wizard")
//
//    override var keywords = NewProjectWizardStep.Keywords()
//
//    override fun setupUI(builder: Panel) {
//        builder.apply {
//            row {
//                comment("hello, world")
//            }
//        }
//    }
//}

//class MoveLangModuleBuilderFromAdapter: GeneratorNewProjectWizardBuilderAdapter(MoveLangProjectWizard())

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
