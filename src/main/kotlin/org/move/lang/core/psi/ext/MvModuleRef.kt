package org.move.lang.core.psi.ext

//val MvModuleRef.isSelfModuleRef: Boolean
//    get() =
//        this !is MvFQModuleRef
//                && this.referenceName == "Self"
//                && this.containingModule != null

//abstract class MvModuleRefMixin(node: ASTNode) : MvElementImpl(node), MvModuleRef {
//
//    override fun getReference(): MvPolyVariantReference? = MvModuleReferenceImpl(this)
//}

//abstract class MvImportedModuleRefMixin(node: ASTNode) : MvReferenceElementImpl(node),
//                                                           MvImportedModuleRef {
//    override val identifier: PsiElement
//        get() {
//            throw NotImplementedError()
////            if (this is MvImportedModuleRef) return this.identifier
////            if (this is MvFQModuleRef) return this.identifier
//////            if (self is MvImportedModuleRef
//////                || self is MvFQModuleRef) return self.identifier
////            return null
//        }
//
//    override fun getReference(): MvReference {
//        return MvModuleReferenceImpl(this)
//    }
//
//    override val isUnresolved: Boolean
//        get() = super<MvReferenceElementImpl>.isUnresolved
//}
