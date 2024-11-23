package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.IStubElementType
import org.move.ide.MoveIcons
import org.move.lang.core.psi.MvModule
import org.move.lang.core.psi.MvModuleItemSpec
import org.move.lang.core.psi.MvModuleSpec
import org.move.lang.core.psi.MvModuleSpecBlock
import org.move.lang.core.psi.MvSpecCodeBlock
import org.move.lang.core.psi.MvSpecFunction
import org.move.lang.core.psi.MvSpecInlineFunction
import org.move.lang.core.psi.containingModule
import org.move.lang.core.psi.impl.MvNameIdentifierOwnerImpl
import org.move.lang.core.psi.namespaceModule
import org.move.lang.core.stubs.MvSpecFunctionStub
import org.move.lang.core.stubs.MvStubbedNamedElementImpl
import org.move.lang.core.types.ItemQualName
import javax.swing.Icon

val MvSpecFunction.parentModule: MvModule? get() {
    val parent = this.parent
    if (parent is MvModule) return parent
    if (parent is MvModuleSpecBlock) {
        return parent.moduleSpec.moduleItem
    }
    return null
}

val MvSpecInlineFunction.parentModule: MvModule? get() {
    val specCodeBlock = this.parent.parent as MvSpecCodeBlock
    val moduleSpec = specCodeBlock.parent as? MvModuleItemSpec ?: return null
    return moduleSpec.namespaceModule
}

abstract class MvSpecFunctionMixin: MvStubbedNamedElementImpl<MvSpecFunctionStub>,
                                    MvSpecFunction {

    constructor(node: ASTNode): super(node)

    constructor(stub: MvSpecFunctionStub, nodeType: IStubElementType<*, *>): super(stub, nodeType)

    override val qualName: ItemQualName?
        get() {
            val itemName = this.name ?: return null
            val moduleFQName = this.parentModule?.qualName ?: return null
            return ItemQualName(this, moduleFQName.address, moduleFQName.itemName, itemName)
        }

    override fun getIcon(flags: Int): Icon = MoveIcons.FUNCTION
}

abstract class MvSpecInlineFunctionMixin(node: ASTNode): MvNameIdentifierOwnerImpl(node),
                                                         MvSpecInlineFunction {


    override val qualName: ItemQualName?
        get() {
            val itemName = this.name ?: return null
            val moduleFQName = this.parentModule?.qualName ?: return null
            return ItemQualName(this, moduleFQName.address, moduleFQName.itemName, itemName)
        }

    override fun getIcon(flags: Int): Icon = MoveIcons.FUNCTION
}
