package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.IStubElementType
import org.move.lang.core.psi.MvModuleSpec
import org.move.lang.core.stubs.MvModuleSpecStub
import org.move.lang.core.stubs.MvStubbedElementImpl

abstract class MvModuleSpecMixin : MvStubbedElementImpl<MvModuleSpecStub>, MvModuleSpec {

    constructor(node: ASTNode) : super(node)

    constructor(stub: MvModuleSpecStub, nodeType: IStubElementType<*, *>) : super(stub, nodeType)
}
