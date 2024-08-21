package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.move.lang.core.psi.MvElementImpl
import org.move.lang.core.psi.MvPatFieldFull
import org.move.lang.core.psi.MvNamedElement
import org.move.lang.core.psi.MvPatStruct
import org.move.lang.core.resolve.collectResolveVariants
import org.move.lang.core.resolve.ref.MvPolyVariantReference
import org.move.lang.core.resolve.ref.MvPolyVariantReferenceCached
import org.move.lang.core.resolve.ref.ResolveCacheDependency
import org.move.lang.core.resolve2.processStructPatFieldResolveVariants

val MvPatFieldFull.parentPatStruct: MvPatStruct get() = ancestorStrict()!!

abstract class MvPatFieldFullMixin(node: ASTNode): MvElementImpl(node),
                                                   MvPatFieldFull {

    override val referenceNameElement: PsiElement get() = this.identifier

    override fun getReference(): MvPolyVariantReference =
        object: MvPolyVariantReferenceCached<MvPatFieldFull>(this@MvPatFieldFullMixin) {
            override fun multiResolveInner(): List<MvNamedElement> =
                collectResolveVariants(element.referenceName) {
                    processStructPatFieldResolveVariants(element, it)
                }

            override val cacheDependency: ResolveCacheDependency
                get() = ResolveCacheDependency.LOCAL_AND_RUST_STRUCTURE
        }
}
