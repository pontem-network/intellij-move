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
import org.move.cli.settings.AptosSettingsPanel
import org.move.cli.settings.moveSettings
import org.move.openapiext.isFeatureEnabled
import javax.swing.JComponent

class AptosConfigurationWizardStep(
    private val context: WizardContext,
    private val configurationUpdaterConsumer: ((ModuleBuilder.ModuleConfigurationUpdater) -> Unit)? = null
) : ModuleWizardStep() {

    private val aptosSettingsPanel = AptosSettingsPanel(showDefaultProjectSettingsLink = true)

    override fun getComponent(): JComponent =
        panel {
            aptosSettingsPanel.attachTo(this)
        }.withBorderIfNeeded()

    override fun disposeUIResources() = Disposer.dispose(aptosSettingsPanel)

    override fun updateDataModel() {
        val panelData = aptosSettingsPanel.panelData
        ConfigurationUpdater.data = panelData

        val projectBuilder = context.projectBuilder
        if (projectBuilder is AptosModuleBuilder) {
            projectBuilder.configurationData = panelData
            projectBuilder.addModuleConfigurationUpdater(ConfigurationUpdater)
        } else {
            configurationUpdaterConsumer?.invoke(ConfigurationUpdater)
        }
    }

    // It's simple hack to imitate new UI style if new project wizard is enabled
    // TODO: drop it and support new project wizard properly
    //  see https://github.com/intellij-rust/intellij-rust/issues/8585
    private fun <T : JComponent> T.withBorderIfNeeded(): T {
        if (isNewWizard()) {
            // border size is taken from `com.intellij.ide.wizard.NewProjectWizardStepPanel`
            border = JBUI.Borders.empty(14, 20)
        }
        return this
    }

    private fun isNewWizard(): Boolean = isFeatureEnabled("new.project.wizard")

    private object ConfigurationUpdater : ModuleBuilder.ModuleConfigurationUpdater() {
        var data: AptosSettingsPanel.PanelData? = null

        override fun update(module: Module, rootModel: ModifiableRootModel) {
            val data = data
            if (data != null) {
                module.project.moveSettings.modify {
                    it.aptosPath = data.aptosExec.pathToSettingsFormat()
                }
            }
            // We don't use SDK, but let's inherit one to reduce the amount of
            // "SDK not configured" errors
            // https://github.com/intellij-rust/intellij-rust/issues/1062
            rootModel.inheritSdk()
            module.project.moveProjectsService.scheduleProjectsRefresh()

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
