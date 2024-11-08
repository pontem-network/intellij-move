package org.move.lang.core.psi.ext

import com.intellij.ide.projectView.PresentationData
import com.intellij.lang.ASTNode
import com.intellij.navigation.ItemPresentation
import com.intellij.psi.stubs.IStubElementType
import org.move.ide.MoveIcons
import org.move.lang.core.psi.MvConst
import org.move.lang.core.psi.MvModule
import org.move.lang.core.stubs.MvConstStub
import org.move.lang.core.stubs.MvStubbedNamedElementImpl
import org.move.lang.core.types.ItemQualName
import javax.swing.Icon

val MvConst.module: MvModule? get() = this.parent as? MvModule

abstract class MvConstMixin : MvStubbedNamedElementImpl<MvConstStub>,
                              MvConst {

    constructor(node: ASTNode) : super(node)

    constructor(stub: MvConstStub, nodeType: IStubElementType<*, *>) : super(stub, nodeType)

    override val qualName: ItemQualName?
        get() {
            val itemName = this.name ?: return null
            val moduleFQName = this.module?.qualName ?: return null
            return ItemQualName(this, moduleFQName.address, moduleFQName.itemName, itemName)
        }

    override fun getIcon(flags: Int): Icon? = MoveIcons.CONST
}
