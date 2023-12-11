package org.move.ide.newProject.module

import com.intellij.ide.util.projectWizard.ModuleBuilder
import com.intellij.ide.util.projectWizard.ModuleWizardStep
import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.openapi.util.Disposer
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.JBUI
import org.move.cli.moveProjectsService
import org.move.cli.settings.AptosExec
import org.move.cli.settings.ChooseAptosCliPanel
import org.move.cli.settings.moveSettings
import org.move.openapiext.isFeatureEnabled
import javax.swing.JComponent

class MoveLangConfigurationWizardStep(
    private val context: WizardContext,
    private val configurationUpdaterConsumer: ((ModuleBuilder.ModuleConfigurationUpdater) -> Unit)? = null
): ModuleWizardStep() {

    private val chooseAptosCliPanel = ChooseAptosCliPanel(showDefaultProjectSettingsLink = true)

    override fun getComponent(): JComponent =
        panel {
            chooseAptosCliPanel.attachToLayout(this)
        }.withBorderIfNeeded()

    override fun disposeUIResources() = Disposer.dispose(chooseAptosCliPanel)

    override fun updateDataModel() {
        val selectedAptosExec = chooseAptosCliPanel.selectedAptosExec
        ConfigurationUpdater.aptosExec = selectedAptosExec

        val projectBuilder = context.projectBuilder
        if (projectBuilder is MoveLangModuleBuilder) {
            projectBuilder.aptosExec = selectedAptosExec
            projectBuilder.addModuleConfigurationUpdater(ConfigurationUpdater)
        } else {
            configurationUpdaterConsumer?.invoke(ConfigurationUpdater)
        }
    }

    // It's simple hack to imitate new UI style if new project wizard is enabled
    // TODO: drop it and support new project wizard properly
    //  see https://github.com/intellij-rust/intellij-rust/issues/8585
    private fun <T: JComponent> T.withBorderIfNeeded(): T {
        if (isNewWizard()) {
            // border size is taken from `com.intellij.ide.wizard.NewProjectWizardStepPanel`
            border = JBUI.Borders.empty(14, 20)
        }
        return this
    }

    private fun isNewWizard(): Boolean = isFeatureEnabled("new.project.wizard")

    private object ConfigurationUpdater: ModuleBuilder.ModuleConfigurationUpdater() {
        var aptosExec: AptosExec? = null

        override fun update(module: Module, rootModel: ModifiableRootModel) {
            val aptosExec = aptosExec
            if (aptosExec != null) {
                module.project.moveSettings.modify {
                    it.aptosPath = aptosExec.pathToSettingsFormat()
                }
            }
            // We don't use SDK, but let's inherit one to reduce the amount of
            // "SDK not configured" errors
            // https://github.com/intellij-rust/intellij-rust/issues/1062
            rootModel.inheritSdk()
            module.project.moveProjectsService.scheduleProjectsRefresh("IDEA New Project generator finished")

//            val contentEntry = rootModel.contentEntries.singleOrNull()
//            if (contentEntry != null) {
//                val manifest = contentEntry.file?.findChild(MoveConstants.MANIFEST_FILE)
////                if (manifest != null) {
////                    module.project.move.attachCargoProject(manifest.pathAsPath)
////                }
//
//                val projectRoot = contentEntry.file ?: return
//                contentEntry.setup(projectRoot)
//            }
        }
    }
}
