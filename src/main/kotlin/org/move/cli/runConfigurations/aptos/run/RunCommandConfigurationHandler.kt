package org.move.cli.runConfigurations.aptos.run

import com.intellij.psi.stubs.StubIndex
import org.move.cli.MoveProject
import org.move.cli.runConfigurations.aptos.FunctionCallConfigurationHandler
import org.move.lang.core.psi.MvFunction
import org.move.lang.core.psi.ext.isEntry
import org.move.lang.index.MvEntryFunctionIndex
import org.move.lang.moveProject

class RunCommandConfigurationHandler : FunctionCallConfigurationHandler() {

    override val subCommand: String get() = "move run"

    override fun getFunction(moveProject: MoveProject, functionQualName: String): MvFunction? {
        return getEntryFunction(moveProject, functionQualName)
    }

    override fun getFunctionByCmdName(moveProject: MoveProject, functionCmdName: String): MvFunction? {
        return getEntryFunction(moveProject, functionCmdName)
    }

    override fun getFunctionCompletionVariants(moveProject: MoveProject): Collection<String> {
//        println("fetch completion variants")
        return MvEntryFunctionIndex.getAllKeys(moveProject.project)
//        val completionVariants = mutableListOf<String>()
//        for (key in keys) {
//            val functions =
//                StubIndex.getElements(MvEntryFunctionIndex.KEY, key, moveProject.project, null, MvFunction::class.java)
//            for (function in functions) {
//                if (function.moveProject != moveProject) continue
//                val qualName = function.qualName?.editorText() ?: continue
//                completionVariants.add(qualName)
//            }
//        }
//        return completionVariants
    }

    companion object {
        fun getEntryFunction(moveProject: MoveProject, functionId: String): MvFunction? {
            return MvEntryFunctionIndex.getFunctionByFunctionId(
                moveProject,
                functionId,
                itemFilter = { fn -> fn.isEntry }
            )
        }
    }
}
