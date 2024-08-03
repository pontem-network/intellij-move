package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.IStubElementType
import org.move.ide.MoveIcons
import org.move.lang.core.psi.MvEnumVariant
import org.move.lang.core.stubs.MvEnumVariantStub
import org.move.lang.core.stubs.MvStubbedNamedElementImpl
import javax.swing.Icon

abstract class MvEnumVariantMixin: MvStubbedNamedElementImpl<MvEnumVariantStub>,
                                   MvEnumVariant {
    constructor(node: ASTNode): super(node)

    constructor(stub: MvEnumVariantStub, nodeType: IStubElementType<*, *>): super(stub, nodeType)

    override fun getIcon(flags: Int): Icon = MoveIcons.STRUCT
}