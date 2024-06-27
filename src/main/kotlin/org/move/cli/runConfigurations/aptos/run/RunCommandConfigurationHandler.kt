package org.move.cli.runConfigurations.aptos.run

import org.move.cli.MoveProject
import org.move.cli.runConfigurations.aptos.CommandConfigurationHandler
import org.move.lang.core.psi.MvFunction
import org.move.lang.core.psi.MvFunctionParameter
import org.move.lang.core.psi.ext.hasTestAttr
import org.move.lang.core.psi.ext.isEntry
import org.move.lang.core.psi.ext.transactionParameters
import org.move.lang.index.MvEntryFunctionIndex

class RunCommandConfigurationHandler : CommandConfigurationHandler() {

    override val subCommand: String get() = "move run"

    override fun configurationName(functionId: String): String = "Run $functionId"

    override fun functionPredicate(function: MvFunction): Boolean = function.isEntry && !function.hasTestAttr

    override fun getFunctionItem(moveProject: MoveProject, functionQualName: String): MvFunction? {
        return getEntryFunction(moveProject, functionQualName)
    }

    override fun getFunctionByCmdName(moveProject: MoveProject, functionCmdName: String): MvFunction? {
        return getEntryFunction(moveProject, functionCmdName)
    }

    override fun getFunctionParameters(function: MvFunction): List<MvFunctionParameter> {
        return function.transactionParameters
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
