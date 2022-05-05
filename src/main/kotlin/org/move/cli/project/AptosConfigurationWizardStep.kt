package org.move.cli.project

import com.intellij.ide.util.projectWizard.ModuleBuilder
import com.intellij.ide.util.projectWizard.ModuleWizardStep
import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.openapi.util.Disposer
import com.intellij.ui.layout.panel
import com.intellij.util.ui.JBUI
import org.move.cli.moveProjects
import org.move.cli.settings.AptosSettingsPanel
import org.move.cli.settings.moveSettings
import org.move.ide.newProject.ConfigurationData
import org.move.openapiext.isFeatureEnabled
import javax.swing.JComponent

class AptosConfigurationWizardStep(
    private val context: WizardContext,
    private val configurationUpdaterConsumer: ((ModuleBuilder.ModuleConfigurationUpdater) -> Unit)? = null
) : ModuleWizardStep() {

    private val aptosSettingsPanel = AptosSettingsPanel()

    override fun getComponent(): JComponent = panel {
        aptosSettingsPanel.attachTo(this)
    }.withBorderIfNeeded()

    override fun disposeUIResources() = Disposer.dispose(aptosSettingsPanel)

    override fun updateDataModel() {
        val data = aptosSettingsPanel.data
        ConfigurationUpdater.data = data

        val projectBuilder = context.projectBuilder
        if (projectBuilder is MvModuleBuilder) {
            projectBuilder.configurationData = data
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
        var data: ConfigurationData? = null

        override fun update(module: Module, rootModel: ModifiableRootModel) {
            val data = data
            if (data != null) {
                module.project.moveSettings.modify {
                    it.aptosPath = data.aptosPath
                }
            }
            // We don't use SDK, but let's inherit one to reduce the amount of
            // "SDK not configured" errors
            // https://github.com/intellij-rust/intellij-rust/issues/1062
            rootModel.inheritSdk()
            module.project.moveProjects.refreshAllProjects()

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
