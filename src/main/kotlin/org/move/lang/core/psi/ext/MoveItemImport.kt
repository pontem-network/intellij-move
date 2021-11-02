package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.move.lang.core.psi.MoveItemImport
import org.move.lang.core.psi.MoveModuleItemsImport
import org.move.lang.core.psi.MoveNamedElement
import org.move.lang.core.psi.impl.MoveNamedElementImpl
import org.move.lang.core.resolve.ref.*

fun MoveItemImport.moduleImport(): MoveModuleItemsImport =
    ancestorStrict() ?: error("ItemImport outside ModuleItemsImport")

abstract class MoveItemImportMixin(node: ASTNode) : MoveNamedElementImpl(node),
                                                    MoveItemImport {
    override fun getName(): String? {
        val name = super.getName()
        if (name != "Self") return name
        return ancestorStrict<MoveModuleItemsImport>()?.fqModuleRef?.referenceName ?: name
    }

    override val referenceNameElement: PsiElement get() = identifier

    override fun getReference(): MoveReference {
        val moduleRef = moduleImport().fqModuleRef
        val itemImport = this
        return object : MoveReferenceBase<MoveItemImport>(itemImport) {
            override fun resolve(): MoveNamedElement? {
                val refName = referenceName ?: return null
                val module = moduleRef.reference?.resolve() ?: return null
                val vs = Visibility.buildSetOfVisibilities(moduleRef)
                return resolveModuleItem(
                    module,
                    refName,
                    vs,
                    setOf(Namespace.TYPE, Namespace.NAME, Namespace.SCHEMA)
                )
            }
        }
    }
}
