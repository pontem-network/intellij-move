package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.IStubElementType
import org.move.ide.MoveIcons
import org.move.lang.core.psi.MvConst
import org.move.lang.core.psi.MvModule
import org.move.lang.core.stubs.MvConstStub
import org.move.lang.core.stubs.MvStubbedNamedElementImpl
import org.move.lang.core.types.ItemQualName
import javax.swing.Icon

val MvConst.module: MvModule?
    get() {
        val moduleBlock = this.parent
        return moduleBlock.parent as? MvModule
    }

abstract class MvConstMixin : MvStubbedNamedElementImpl<MvConstStub>,
                              MvConst {

    constructor(node: ASTNode) : super(node)

    constructor(stub: MvConstStub, nodeType: IStubElementType<*, *>) : super(stub, nodeType)

    override val qualName: ItemQualName
        get() {
            val moduleFQName = this.module?.qualName ?: ItemQualName.ANY_SCRIPT
            val itemName = this.name ?: "<unknown>"
            return ItemQualName(moduleFQName.address, moduleFQName.itemName, itemName)
        }

    override fun getIcon(flags: Int): Icon? = MoveIcons.CONST
}
