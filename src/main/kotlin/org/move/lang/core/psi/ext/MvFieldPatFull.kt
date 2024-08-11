package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.move.lang.core.psi.MvElementImpl
import org.move.lang.core.psi.MvFieldPatFull
import org.move.lang.core.psi.MvNamedElement
import org.move.lang.core.psi.MvStructPat
import org.move.lang.core.resolve.collectResolveVariants
import org.move.lang.core.resolve.ref.MvPolyVariantReference
import org.move.lang.core.resolve.ref.MvPolyVariantReferenceCached
import org.move.lang.core.resolve.ref.ResolveCacheDependency
import org.move.lang.core.resolve2.processStructPatFieldResolveVariants

val MvFieldPatFull.parentStructPat: MvStructPat get() = ancestorStrict()!!

abstract class MvFieldPatFullMixin(node: ASTNode): MvElementImpl(node),
                                                   MvFieldPatFull {

    override val referenceNameElement: PsiElement get() = this.identifier

    override fun getReference(): MvPolyVariantReference =
        object: MvPolyVariantReferenceCached<MvFieldPatFull>(this@MvFieldPatFullMixin) {
            override fun multiResolveInner(): List<MvNamedElement> =
                collectResolveVariants(element.referenceName) {
                    processStructPatFieldResolveVariants(element, it)
                }

            override val cacheDependency: ResolveCacheDependency
                get() = ResolveCacheDependency.LOCAL_AND_RUST_STRUCTURE
        }
}
