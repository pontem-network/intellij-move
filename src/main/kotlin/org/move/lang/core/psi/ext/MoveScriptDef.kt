package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import org.move.lang.core.psi.*

fun MvScriptDef.allFunctions(): List<MvFunction> = scriptBlock?.functionList.orEmpty()

fun MvScriptDef.constBindings(): List<MvBindingPat> =
    scriptBlock?.constDefList.orEmpty().mapNotNull { it.bindingPat }

//fun MvScriptDef.builtinFunctions(): List<MvFunction> {
//    return listOf(
//        createBuiltinFunction("native fun assert(_: bool, err: u64);", project)
//    )
//}

abstract class MvScriptDefMixin(node: ASTNode) : MvElementImpl(node),
                                                 MvScriptDef {
    override val importStatements: List<MvImportStatement>
        get() =
            this.scriptBlock?.importStatementList.orEmpty()
}
