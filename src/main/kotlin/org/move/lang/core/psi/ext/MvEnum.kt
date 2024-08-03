package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.IStubElementType
import org.move.ide.MoveIcons
import org.move.lang.core.psi.MvEnum
import org.move.lang.core.psi.MvEnumVariant
import org.move.lang.core.stubs.MvEnumStub
import org.move.lang.core.stubs.MvStubbedNamedElementImpl
import javax.swing.Icon

val MvEnum.variants: List<MvEnumVariant> get() = enumBody?.enumVariantList.orEmpty()

abstract class MvEnumMixin: MvStubbedNamedElementImpl<MvEnumStub>,
                            MvEnum {
    constructor(node: ASTNode): super(node)

    constructor(stub: MvEnumStub, nodeType: IStubElementType<*, *>): super(stub, nodeType)

    override fun getIcon(flags: Int): Icon = MoveIcons.STRUCT
}