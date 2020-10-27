package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import org.move.lang.core.psi.MoveItemImport
import org.move.lang.core.psi.MoveModuleItemsImport
import org.move.lang.core.psi.impl.MoveNamedElementImpl

fun MoveItemImport.parentImport(): MoveModuleItemsImport =
    ancestorStrict() ?: error("ItemImport outside ModuleItemsImport")

abstract class MoveItemImportMixin(node: ASTNode) : MoveNamedElementImpl(node),
                                                    MoveItemImport {
    override fun getName(): String? {
        val name = super.getName()
        if (name != "Self") return name
        return ancestorStrict<MoveModuleItemsImport>()?.fullyQualifiedModuleRef?.referenceName ?: name
    }

//    override val referenceNameElement: PsiElement get() = identifier

//    override fun getReference(): MoveReference {
//        val moduleRef = parentImport().fullyQualifiedModuleRef
//
//        val itemImport = this
//        return object : MoveReferenceBase<MoveItemImport>(itemImport) {
//            override fun resolve(): MoveNamedElement? {
//                return resolveQualifiedPath(moduleRef,
//                    referenceName,
//                    setOf(Namespace.TYPE, Namespace.NAME, Namespace.SCHEMA))
//            }
//        }
//    }


//    override fun getReference(): MoveReference {
//        return MoveItemImportReferenceImpl(this)
//    }
}