package org.move.lang.core.psi.ext

import com.intellij.ide.projectView.PresentationData
import com.intellij.lang.ASTNode
import com.intellij.navigation.ItemPresentation
import com.intellij.psi.stubs.IStubElementType
import org.move.ide.MoveIcons
import org.move.lang.core.psi.MvEnum
import org.move.lang.core.psi.MvEnumBody
import org.move.lang.core.psi.MvEnumVariant
import org.move.lang.core.stubs.MvEnumVariantStub
import org.move.lang.core.stubs.MvStubbedNamedElementImpl
import javax.swing.Icon

val MvEnumVariant.enumBody: MvEnumBody get() = this.parent as MvEnumBody
val MvEnumVariant.enumItem: MvEnum get() = this.enumBody.parent as MvEnum

abstract class MvEnumVariantMixin: MvStubbedNamedElementImpl<MvEnumVariantStub>,
                                   MvEnumVariant {
    constructor(node: ASTNode): super(node)

    constructor(stub: MvEnumVariantStub, nodeType: IStubElementType<*, *>): super(stub, nodeType)

    override fun getIcon(flags: Int): Icon = MoveIcons.STRUCT

    override fun getPresentation(): ItemPresentation? {
        val variant = this
        val variantName = this.name ?: return null
        val presentationText = buildString {
            append(variantName)
            val fields = variant.tupleFields
            if (fields != null) {
                append('(')
                val xs = fields.tupleFieldDeclList.map { it.type.text }
                append(xs.joinToString(", "))
                append(')')
            }
        }
        return PresentationData(
            presentationText,
            this.locationString(true),
            MoveIcons.ENUM_VARIANT,
            null
        )
    }
}