package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import com.intellij.navigation.ItemPresentation
import com.intellij.psi.stubs.IStubElementType
import org.move.ide.MoveIcons
import org.move.lang.core.psi.MvModule
import org.move.lang.core.psi.MvSpecFunction
import org.move.lang.core.psi.MvSpecInlineFunction
import org.move.lang.core.psi.impl.MvNameIdentifierOwnerImpl
import org.move.lang.core.psi.itemPresentation
import org.move.lang.core.stubs.MvSpecFunctionStub
import org.move.lang.core.stubs.MvStubbedNamedElementImpl
import org.move.lang.core.types.ItemQualName
import javax.swing.Icon

val MvSpecFunction.module: MvModule?
    get() {
//        val moduleStub = greenStub?.parentStub as? MvModuleStub
//        if (moduleStub != null) {
//            return moduleStub.psi
//        }
        return this.parent.parent as? MvModule
    }

abstract class MvSpecFunctionMixin : MvStubbedNamedElementImpl<MvSpecFunctionStub>,
                                     MvSpecFunction {

    constructor(node: ASTNode) : super(node)

    constructor(stub: MvSpecFunctionStub, nodeType: IStubElementType<*, *>) : super(stub, nodeType)

    override val qualName: ItemQualName?
        get() {
            val itemName = this.name ?: return null
            val moduleFQName = this.module?.qualName ?: return null
            return ItemQualName(this, moduleFQName.address, moduleFQName.itemName, itemName)
        }

    override fun getIcon(flags: Int): Icon = MoveIcons.FUNCTION

    override fun getPresentation(): ItemPresentation? = this.itemPresentation

}

abstract class MvSpecInlineFunctionMixin(node: ASTNode) : MvNameIdentifierOwnerImpl(node),
                                                          MvSpecInlineFunction {

    override fun getIcon(flags: Int): Icon = MoveIcons.FUNCTION

    override fun getPresentation(): ItemPresentation? = this.itemPresentation
}
