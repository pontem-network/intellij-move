package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.IStubElementType
import org.move.ide.MoveIcons
import org.move.lang.core.psi.MvConst
import org.move.lang.core.psi.MvModule
import org.move.lang.core.stubs.MvConstStub
import org.move.lang.core.stubs.MvStubbedNamedElementImpl
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

    override val fqName: String
        get() {
            val moduleFqName = this.module?.fqName?.let { "$it::" }
            val name = this.name ?: "<unknown>"
            return moduleFqName + name
        }

    override fun getIcon(flags: Int): Icon? = MoveIcons.CONST
}
