package org.move.lang.core.psi.ext

import org.move.lang.core.psi.MvBindingPat
import org.move.lang.core.psi.MvConst
import org.move.lang.core.psi.MvFunction
import org.move.lang.core.psi.MvScript

fun MvScript.allFunctions(): List<MvFunction> = scriptBlock?.functionList.orEmpty()

fun MvScript.consts(): List<MvConst> = scriptBlock?.constList.orEmpty()

//fun MvScriptDef.builtinFunctions(): List<MvFunction> {
//    return listOf(
//        createBuiltinFunction("native fun assert(_: bool, err: u64);", project)
//    )
//}

//abstract class MvScriptMixin(node: ASTNode) : MvElementImpl(node),
//                                              MvScript
