package org.move.ide.newProject.module

import com.intellij.ide.util.projectWizard.ModuleBuilder
import com.intellij.ide.util.projectWizard.ModuleWizardStep
import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.module.ModuleType
import com.intellij.openapi.projectRoots.SdkTypeId
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.openapi.util.Disposer
import org.move.cli.Consts
import org.move.cli.runConfigurations.addCompileProjectRunConfiguration
import org.move.cli.runConfigurations.aptos.AptosCliExecutor
import org.move.cli.settings.AptosExec
import org.move.ide.newProject.openFile
import org.move.openapiext.computeWithCancelableProgress
import org.move.stdext.unwrapOrThrow

class MoveLangModuleBuilder : ModuleBuilder() {
    override fun getModuleType(): ModuleType<*> = MoveLangModuleType.Util.INSTANCE

    override fun isSuitableSdkType(sdkType: SdkTypeId?): Boolean = true

    override fun getCustomOptionsStep(
        context: WizardContext,
        parentDisposable: Disposable
    ): ModuleWizardStep {
        return AptosConfigurationWizardStep(context).apply {
            Disposer.register(parentDisposable, this::disposeUIResources)
        }
    }

    override fun setupRootModel(modifiableRootModel: ModifiableRootModel) {
        val root = doAddContentEntry(modifiableRootModel)?.file ?: return
        modifiableRootModel.inheritSdk()

        val aptosPath = aptosExec?.toPathOrNull()
        root.refresh(false, true)

        // Just work if user "creates new project" over an existing one.
        if (aptosPath != null && root.findChild(Consts.MANIFEST_FILE) == null) {
            val aptosCli = AptosCliExecutor(aptosPath)
            val project = modifiableRootModel.project
            val packageName = project.name.replace(' ', '_')

            ApplicationManager.getApplication().executeOnPooledThread {
                val manifestFile = project.computeWithCancelableProgress("Generating Aptos project...") {
                    aptosCli.moveInit(
                        project,
                        modifiableRootModel.module,
                        rootDirectory = root,
                        packageName = packageName
                    )
                        .unwrapOrThrow() // TODO throw? really??
                }
                project.addCompileProjectRunConfiguration(true)
                project.openFile(manifestFile)
            }
        }
    }

    var aptosExec: AptosExec? = null
}
