package org.move.lang.core.resolve

import org.move.lang.core.psi.MoveNamedElement

interface MoveDefsResolveScope : MoveResolveScope {
    fun definitions(): List<MoveNamedElement>
}

//private inline fun <reified I : MoveDefElement> MoveDefsResolveScope.defs(): List<I> =
//    PsiTreeUtil.getChildrenOfTypeAsList(this, I::class.java)

//val MoveDefsResolveScope.allDefinitions: List<MoveNamedElement>
//    get() = listOf<List<MoveNamedElement>>(
//        defs<MoveFunctionDef>(),
//        defs<MoveNativeFunctionDef>()
//    ).flatten()



