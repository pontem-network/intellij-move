package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.move.lang.core.psi.MvItemUse
import org.move.lang.core.psi.MvModuleDef
import org.move.lang.core.psi.MvModuleItemUse
import org.move.lang.core.psi.MvNamedElement
import org.move.lang.core.psi.impl.MvNamedElementImpl
import org.move.lang.core.resolve.ItemVis
import org.move.lang.core.resolve.MslScope
import org.move.lang.core.resolve.ref.*

fun MvItemUse.moduleImport(): MvModuleItemUse =
    ancestorStrict() ?: error("ItemImport outside ModuleItemsImport")

abstract class MvItemUseMixin(node: ASTNode) : MvNamedElementImpl(node),
                                               MvItemUse {
    override fun getName(): String? {
        val name = super.getName()
        if (name != "Self") return name
        return ancestorStrict<MvModuleItemUse>()?.fqModuleRef?.referenceName ?: name
    }

    override val referenceNameElement: PsiElement get() = identifier

    override fun getReference(): MvReference {
        val moduleRef = moduleImport().fqModuleRef
        val itemImport = this
        return object : MvReferenceCached<MvItemUse>(itemImport) {
            override fun resolveInner(): List<MvNamedElement> {
                val module = moduleRef.reference?.resolve() as? MvModuleDef ?: return emptyList()
                val ns = setOf(Namespace.TYPE, Namespace.NAME, Namespace.SCHEMA)
                val vs = Visibility.buildSetOfVisibilities(moduleRef)
                val itemVis = ItemVis(ns, vs, MslScope.NONE)
                return resolveModuleItem(
                    module,
                    referenceName,
                    itemVis
                )
            }
        }
    }
}
