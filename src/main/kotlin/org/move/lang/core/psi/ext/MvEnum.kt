package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.IStubElementType
import org.move.ide.MoveIcons
import org.move.lang.core.psi.MvEnum
import org.move.lang.core.psi.MvEnumVariant
import org.move.lang.core.stubs.MvEnumStub
import org.move.lang.core.stubs.MvStubbedNamedElementImpl
import org.move.lang.core.types.ItemQualName
import org.move.lang.core.types.MvPsiTypeImplUtil
import org.move.lang.core.types.ty.Ty
import javax.swing.Icon

val MvEnum.variants: List<MvEnumVariant> get() = enumBody?.enumVariantList.orEmpty()

val MvEnum.tupleVariants: List<MvEnumVariant> get() = variants.filter { it.tupleFields != null }
val MvEnum.structVariants: List<MvEnumVariant> get() = variants.filter { it.blockFields != null }
val MvEnum.unitVariants: List<MvEnumVariant>
    get() =
        variants.filter { it.tupleFields == null && it.blockFields == null }

abstract class MvEnumMixin: MvStubbedNamedElementImpl<MvEnumStub>,
                            MvEnum {
    constructor(node: ASTNode): super(node)

    constructor(stub: MvEnumStub, nodeType: IStubElementType<*, *>): super(stub, nodeType)

    override fun declaredType(msl: Boolean): Ty = MvPsiTypeImplUtil.declaredType(this)

    override fun getIcon(flags: Int): Icon = MoveIcons.STRUCT

    override val qualName: ItemQualName?
        get() {
            val itemName = this.name ?: return null
            val moduleFQName = this.module.qualName ?: return null
            return ItemQualName(this, moduleFQName.address, moduleFQName.itemName, itemName)
        }
}