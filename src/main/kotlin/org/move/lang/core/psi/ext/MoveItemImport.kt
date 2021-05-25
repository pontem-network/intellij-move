package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.move.lang.core.psi.MoveItemImport
import org.move.lang.core.psi.MoveModuleItemsImport
import org.move.lang.core.psi.MoveNamedElement
import org.move.lang.core.psi.impl.MoveNamedElementImpl
import org.move.lang.core.resolve.ref.MoveReference
import org.move.lang.core.resolve.ref.MoveReferenceBase
import org.move.lang.core.resolve.ref.Namespace
import org.move.lang.core.resolve.ref.resolveQualifiedPath

fun MoveItemImport.parentImport(): MoveModuleItemsImport =
    ancestorStrict() ?: error("ItemImport outside ModuleItemsImport")

abstract class MoveItemImportMixin(node: ASTNode) : MoveNamedElementImpl(node),
                                                    MoveItemImport {
    override fun getName(): String? {
        val name = super.getName()
        if (name != "Self") return name
        return ancestorStrict<MoveModuleItemsImport>()?.fullyQualifiedModuleRef?.referenceName ?: name
    }

    override val referenceNameElement: PsiElement get() = identifier

    override fun getReference(): MoveReference {
        val moduleRef = parentImport().fullyQualifiedModuleRef

        val itemImport = this
        return object : MoveReferenceBase<MoveItemImport>(itemImport) {
            override fun resolve(): MoveNamedElement? {
                val refName = referenceName ?: return null
                return resolveQualifiedPath(
                    moduleRef,
                    refName,
                    setOf(Namespace.TYPE, Namespace.NAME, Namespace.SCHEMA)
                )
            }
        }
    }
}
