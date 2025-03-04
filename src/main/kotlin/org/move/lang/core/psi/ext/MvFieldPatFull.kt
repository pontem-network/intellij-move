package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.move.lang.core.psi.*
import org.move.lang.core.resolve.scopeEntry.filterByName
import org.move.lang.core.resolve.getStructPatFieldResolveVariants
import org.move.lang.core.resolve.ref.MvPolyVariantReference
import org.move.lang.core.resolve.ref.MvPolyVariantReferenceCached
import org.move.lang.core.resolve.ref.ResolveCacheDependency
import org.move.lang.core.resolve.scopeEntry.namedElements

val MvPatFieldFull.patField: MvPatField get() = parent as MvPatField
val MvPatFieldFull.patStruct: MvPatStruct get() = patField.patStruct

abstract class MvPatFieldFullMixin(node: ASTNode): MvElementImpl(node),
                                                   MvPatFieldFull {

    override val referenceNameElement: PsiElement get() = this.identifier

    override fun getReference(): MvPolyVariantReference =
        object: MvPolyVariantReferenceCached<MvPatFieldFull>(this@MvPatFieldFullMixin) {
            override fun multiResolveInner(): List<MvNamedElement> =
                getStructPatFieldResolveVariants(element)
                    .filterByName(element.referenceName).namedElements()

            override val cacheDependency: ResolveCacheDependency
                get() = ResolveCacheDependency.LOCAL_AND_RUST_STRUCTURE
        }
}
