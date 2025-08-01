package org.move.cli.settings.aptos

import com.intellij.ide.DataManager
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.JBPopupFactory.ActionSelectionAid.SPEEDSEARCH
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.io.toNioPathOrNull
import com.intellij.openapi.util.registry.Registry
import com.intellij.ui.components.DropDownLink
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.Row
import org.move.cli.sdks.sdksService
import org.move.cli.settings.VersionLabel
import org.move.cli.settings.isValidExecutable
import org.move.ide.actions.DownloadAptosSDKAction
import org.move.ide.notifications.logOrShowBalloon
import org.move.openapiext.pathField
import org.move.stdext.blankToNull
import org.move.stdext.toPathOrNull
import java.nio.file.Path

object AptosExecType {
    val isPreCompiledSupportedForThePlatform: Boolean
        get() {
            if (Registry.`is`("org.move.aptos.bundled.force.supported", false)) {
                return true
            }
            if (Registry.`is`("org.move.aptos.bundled.force.unsupported", false)) {
                return false
            }
            return !SystemInfo.isMac
        }

    fun aptosCliPath(localAptosPath: String?): Path? {
        return localAptosPath?.blankToNull()
            ?.toPathOrNull()?.takeIf { it.isValidExecutable() }
    }
}

class ChooseAptosCliPanel(versionUpdateListener: (() -> Unit)?): Disposable {

    private val innerDisposable =
        Disposer.newCheckedDisposable("ChooseAptosCliPanel.innerDisposable")

    init {
        Disposer.register(this, innerDisposable)
    }

    data class Data(val aptosPath: String?)

    var data: Data
        get() {
            val path = localPathField.text.blankToNull()
            return Data(path)
        }
        set(value) {
            localPathField.text = value.aptosPath ?: ""
            updateVersion()
        }

    private val localPathField =
        pathField(
            FileChooserDescriptorFactory.createSingleFileOrExecutableAppDescriptor(),
            this,
            "Choose Aptos CLI",
            onTextChanged = { _ ->
                updateVersion()
            })
    private val versionLabel = VersionLabel(innerDisposable, versionUpdateListener = versionUpdateListener)

    private val downloadPrecompiledBinaryAction = DownloadAptosSDKAction().also {
        it.onFinish = { sdk ->
            localPathField.text = sdk.targetFile.toString()
            updateVersion()
        }
    }
    private val downloadPrecompiledActionGroup = DefaultActionGroup(
        listOfNotNull(
            if (AptosExecType.isPreCompiledSupportedForThePlatform) downloadPrecompiledBinaryAction else null
        )
    )
    private val getAptosActionLink =
        DropDownLink("Get Aptos") { dropDownLink ->
            val dataContext = DataManager.getInstance().getDataContext(dropDownLink)
            JBPopupFactory.getInstance().createActionGroupPopup(
                null,
                downloadPrecompiledActionGroup,
                dataContext,
                SPEEDSEARCH,
                false,
                null,
                -1,
                { _ -> false },
                null
            )
        }


    fun attachToLayout(layout: Panel): Row {
        val resultRow = with(layout) {
            group("Aptos CLI") {
                buttonsGroup {
                    row {
                        cell(localPathField)
                            .align(AlignX.FILL)
                            .resizableColumn()
                        if (downloadPrecompiledActionGroup.childrenCount != 0) {
                            cell(getAptosActionLink)
                        }
                    }
                    row("--version :") { cell(versionLabel) }
                }
            }
        }
        updateVersion()
        return resultRow
    }

    private fun updateVersion() {
        val aptosPath = localPathField.text.toNioPathOrNull()
        versionLabel.update(aptosPath)
    }

    fun updateAptosSdks(sdkPath: String) {
        if (sdkPath == "") return

        // do not save if the executable has no `--version`
        if (versionLabel.isError()) return

        // do not save if it's not an aptos
        if ("aptos" !in versionLabel.text) return

        val settingsService = sdksService()
        if (sdkPath in settingsService.state.aptosSdkPaths) return

        settingsService.state.aptosSdkPaths.add(sdkPath)

        LOG.logOrShowBalloon("Aptos SDK saved: $sdkPath")
    }

    override fun dispose() {
        Disposer.dispose(localPathField)
    }

    companion object {
        private val LOG = logger<ChooseAptosCliPanel>()
    }
}
