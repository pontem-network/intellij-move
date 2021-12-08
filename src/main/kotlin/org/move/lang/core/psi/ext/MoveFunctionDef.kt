package org.move.lang.core.psi.ext

import org.move.lang.core.psi.MvFunctionDef

val MvFunctionDef.isTest: Boolean get() =
    this.attrList.any { it.attrItemList.any { item -> item.identifier.text == "test" } }

//val MvFunctionDef.params: List<MvFunctionParameter>
//    get() =
//        emptyList()
//        this.functionParameterList?.functionParameterList.orEmpty()

//val MvFunctionDef.isPublic: Boolean
//    get() = isChildExists(MvElementTypes.PUBLIC)
