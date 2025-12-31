package org.move.cli.settings.endless

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
import org.move.ide.actions.DownloadEndlessSDKAction
import org.move.ide.notifications.logOrShowBalloon
import org.move.openapiext.pathField
import org.move.stdext.blankToNull
import org.move.stdext.toPathOrNull
import java.nio.file.Path

object EndlessExecType {
    val isPreCompiledSupportedForThePlatform: Boolean
        get() {
            if (Registry.`is`("org.move.endless.bundled.force.supported", false)) {
                return true
            }
            if (Registry.`is`("org.move.endless.bundled.force.unsupported", false)) {
                return false
            }
            return !SystemInfo.isMac
        }

    fun endlessCliPath(localEndlessPath: String?): Path? {
        return localEndlessPath?.blankToNull()
            ?.toPathOrNull()?.takeIf { it.isValidExecutable() }
    }
}

class ChooseEndlessCliPanel(versionUpdateListener: (() -> Unit)?): Disposable {

    private val innerDisposable =
        Disposer.newCheckedDisposable("ChooseEndlessCliPanel.innerDisposable")

    init {
        Disposer.register(this, innerDisposable)
    }

    data class Data(val endlessPath: String?)

    var data: Data
        get() {
            val path = localPathField.text.blankToNull()
            return Data(path)
        }
        set(value) {
            localPathField.text = value.endlessPath ?: ""
            updateVersion()
        }

    private val localPathField =
        pathField(
            FileChooserDescriptorFactory.createSingleFileOrExecutableAppDescriptor(),
            this,
            "Choose Endless CLI",
            onTextChanged = { _ ->
                updateVersion()
            })
    private val versionLabel = VersionLabel(innerDisposable, versionUpdateListener = versionUpdateListener)

    private val downloadPrecompiledBinaryAction = DownloadEndlessSDKAction().also {
        it.onFinish = { sdk ->
            localPathField.text = sdk.targetFile.toString()
            updateVersion()
        }
    }
    private val downloadPrecompiledActionGroup = DefaultActionGroup(
        listOfNotNull(
            if (EndlessExecType.isPreCompiledSupportedForThePlatform) downloadPrecompiledBinaryAction else null
        )
    )
    private val getEndlessActionLink =
        DropDownLink("Get Endless") { dropDownLink ->
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
            group("Endless CLI") {
                buttonsGroup {
                    row {
                        cell(localPathField)
                            .align(AlignX.FILL)
                            .resizableColumn()
                        if (downloadPrecompiledActionGroup.childrenCount != 0) {
                            cell(getEndlessActionLink)
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
        val endlessPath = localPathField.text.toNioPathOrNull()
        versionLabel.update(endlessPath)
    }

    fun updateEndlessSdks(sdkPath: String) {
        if (sdkPath == "") return

        // do not save if the executable has no `--version`
        if (versionLabel.isError()) return

        // do not save if it's not an endless
        if ("endless" !in versionLabel.text) return

        val settingsService = sdksService()
        if (sdkPath in settingsService.state.endlessSdkPaths) return

        settingsService.state.endlessSdkPaths.add(sdkPath)

        LOG.logOrShowBalloon("Endless SDK saved: $sdkPath")
    }

    override fun dispose() {
        Disposer.dispose(localPathField)
    }

    companion object {
        private val LOG = logger<ChooseEndlessCliPanel>()
    }
}
