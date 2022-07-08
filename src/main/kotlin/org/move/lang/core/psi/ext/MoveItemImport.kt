package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.move.lang.core.psi.*
import org.move.lang.core.psi.impl.MvNamedElementImpl
import org.move.lang.core.resolve.ItemVis
import org.move.lang.core.resolve.MslScope
import org.move.lang.core.resolve.ref.MvReferenceCached
import org.move.lang.core.resolve.ref.Namespace
import org.move.lang.core.resolve.ref.Visibility
import org.move.lang.core.resolve.ref.resolveModuleItem

fun MvUseItem.moduleImport(): MvItemUseSpeck =
    ancestorStrict() ?: error("ItemImport outside ModuleItemsImport")

val MvUseItem.speck: MvElement?
    get() {
        val parent = this.parent
        if (parent is MvUseItemGroup && parent.useItemList.size != 1) return this
        return ancestorStrict<MvUseStmt>()
    }

val MvUseItem.moduleName: String get() {
    val useStmt = this.ancestorStrict<MvUseStmt>()
    return useStmt?.itemUseSpeck?.fqModuleRef?.referenceName.orEmpty()
}

class MvUseItemReferenceElement(element: MvUseItem) : MvReferenceCached<MvUseItem>(element) {
    override fun resolveInner(): List<MvNamedElement> {
        val moduleRef = element.moduleImport().fqModuleRef
        val module =
            moduleRef.reference?.resolve() as? MvModule ?: return emptyList()
        if ((element.useAlias == null && element.text == "Self")
            || (element.useAlias != null && element.text.startsWith("Self as"))
        ) return listOf(module)

        val ns = setOf(Namespace.TYPE, Namespace.NAME, Namespace.SCHEMA)
        val vs = Visibility.buildSetOfVisibilities(moduleRef)
        val itemVis = ItemVis(
            ns,
            vs,
            MslScope.NONE,
            itemScope = moduleRef.itemScope,
            folderScope = moduleRef.folderScope
        )
        return resolveModuleItem(
            module,
            element.referenceName,
            itemVis
        )
    }

}

abstract class MvUseItemMixin(node: ASTNode) : MvNamedElementImpl(node),
                                               MvUseItem {
    override fun getName(): String? {
        val name = super.getName()
        if (name != "Self") return name
        return ancestorStrict<MvItemUseSpeck>()?.fqModuleRef?.referenceName ?: name
    }

    override val referenceNameElement: PsiElement get() = identifier

    override fun getReference() = MvUseItemReferenceElement(this)
}
