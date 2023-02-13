package org.move.cli.scripts

import org.move.cli.AptosCommandLine
import org.move.cli.toolwindow.MoveProjectsTree
import org.move.cli.toolwindow.MoveProjectsTreeStructure
import org.move.lang.core.psi.MvFunction
import org.move.lang.core.psi.module
import org.move.lang.core.psi.typeParameters
import org.move.lang.core.types.address
import org.move.lang.moveProject
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.SwingUtilities
import javax.swing.tree.DefaultMutableTreeNode

class MoveEntrypointMouseAdapter : MouseAdapter() {
    override fun mouseClicked(e: MouseEvent) {
        if (e.clickCount < 2 || !SwingUtilities.isLeftMouseButton(e)) return

        val tree = e.source as? MoveProjectsTree ?: return
        val node = tree.selectionModel.selectionPath
            ?.lastPathComponent as? DefaultMutableTreeNode ?: return
        val scriptFunction =
            (node.userObject as? MoveProjectsTreeStructure.MoveSimpleNode.Entrypoint)?.function
                ?: return

        val moveProject = scriptFunction.moveProject ?: return
        // TODO: show dialog that user needs to run `aptos init` first for transaction dialog to work
        val aptosConfig = moveProject.currentPackage.aptosConfigYaml ?: return

        val paramsDialog = TransactionParametersDialog(scriptFunction, aptosConfig.profiles.toList())
        val isOk = paramsDialog.showAndGet()
        if (!isOk) return

        buildAndRunAptosCommandLine(scriptFunction, paramsDialog)
    }

    companion object {
        fun buildAndRunAptosCommandLine(
            scriptFunction: MvFunction,
            paramsDialog: TransactionParametersDialog
        ) {
            val moveProj = scriptFunction.moveProject ?: return

            val address = scriptFunction.module?.address(moveProj)?.canonicalValue ?: return
            val module = scriptFunction.module?.name ?: return
            val name = scriptFunction.name ?: return

            val functionTypeParamNames = scriptFunction.typeParameters.mapNotNull { it.name }
            val sortedTypeParams = paramsDialog.typeParams
                .entries
                .sortedBy { (name, _) ->
                    functionTypeParamNames.indexOfFirst { it == name }
                }.flatMap { (_, value) -> listOf("--type-args", maybeQuoteTypeArg(value)) }

            val functionParamNames = scriptFunction.parameterBindings().mapNotNull { it.name }
            val sortedParams = paramsDialog.params.entries
                .sortedBy { (name, _) ->
                    functionParamNames.indexOfFirst { it == name }
                }.flatMap { (_, value) -> listOf("--args", value) }

            val profile = paramsDialog.selectedProfile
            val profileArgs =
                if (profile != null) listOf("--profile", profile) else listOf()
            val commandArgs = listOf(
                profileArgs,
                listOf("--function-id", "${address}::${module}::${name}"),
                sortedTypeParams,
                sortedParams,
            ).flatten()
            AptosCommandLine("move run", moveProj.contentRootPath, commandArgs)
                .run(moveProj, paramsDialog.configurationName)
        }

        fun maybeQuoteTypeArg(typeArg: String): String =
            if (typeArg.contains('<') || typeArg.contains('>')) {
                "\"$typeArg\""
            } else {
                typeArg
            }
    }
}
