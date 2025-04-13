package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.move.ide.MoveIcons
import org.move.lang.MvElementTypes.ENUM_KW
import org.move.lang.core.psi.MvAbilitiesList
import org.move.lang.core.psi.MvEnum
import org.move.lang.core.psi.MvEnumVariant
import org.move.lang.core.psi.impl.MvNameIdentifierOwnerImpl
import javax.swing.Icon

val MvEnum.enumKw: PsiElement? get() = findFirstChildByType(ENUM_KW)
val MvEnum.variants: List<MvEnumVariant> get() = enumBody?.enumVariantList.orEmpty()

val MvEnum.tupleVariants: List<MvEnumVariant> get() = variants.filter { it.tupleFields != null }
val MvEnum.structVariants: List<MvEnumVariant> get() = variants.filter { it.blockFields != null }
val MvEnum.unitVariants: List<MvEnumVariant>
    get() =
        variants.filter { it.tupleFields == null && it.blockFields == null }

abstract class MvEnumMixin(node: ASTNode): MvNameIdentifierOwnerImpl(node),
                                           MvEnum {

    override fun getIcon(flags: Int): Icon = MoveIcons.ENUM

    override val abilitiesList: MvAbilitiesList? get() = abilitiesListList.firstOrNull()
}