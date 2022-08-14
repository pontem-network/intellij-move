package org.move.lang.core.psi.impl

import com.intellij.extapi.psi.StubBasedPsiElementBase
import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.stubs.StubElement
import org.move.lang.core.psi.MvElement

abstract class MvStubbedElementImpl<StubT : StubElement<*>> : StubBasedPsiElementBase<StubT>,
                                                              MvElement {

    constructor(node: ASTNode) : super(node)

    constructor(stub: StubT, nodeType: IStubElementType<*, *>) : super(stub, nodeType)

    override fun toString(): String = "${javaClass.simpleName}($elementType)"
}
