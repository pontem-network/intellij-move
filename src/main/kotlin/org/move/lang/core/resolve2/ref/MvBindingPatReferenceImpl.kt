package org.move.lang.core.resolve2.ref

import com.intellij.psi.PsiElement
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.isMsl
import org.move.lang.core.resolve.ScopeEntry
import org.move.lang.core.resolve.collectResolveVariantsAsScopeEntries
import org.move.lang.core.resolve.ref.*
import org.move.lang.core.resolve2.processPatBindingResolveVariants
import org.move.lang.core.types.infer.inference
import org.move.lang.core.types.ty.Ty
import org.move.stdext.wrapWithList

class MvBindingPatReferenceImpl(
    element: MvPatBinding
): MvPolyVariantReferenceBase<MvPatBinding>(element) {

    override fun multiResolve(): List<MvNamedElement> =
        rawMultiResolveUsingInferenceCache() ?: rawCachedMultiResolve()
//        resolvePatBindingRaw(element, expectedType = null).map { it.element }

//    override fun multiResolveInner(): List<MvNamedElement> =
//        resolvePatBindingRaw(element, expectedType = null).map { it.element }
//        collectResolveVariants(element.referenceName) {
//            processPatBindingResolveVariants(element, false, it)
//        }

    private fun rawMultiResolveUsingInferenceCache(): List<MvNamedElement>? {
        val parent = element.parent as? MvElement ?: return null
        if (parent is MvMatchArm || parent is MvPat) {
            val msl = parent.isMsl()
            return parent.inference(msl)?.getResolvedPatBinding(element).wrapWithList()
        } else {
            return null
        }
    }

    private fun rawCachedMultiResolve(): List<MvNamedElement> {
        return resolvePatBindingRaw(element, expectedType = null).map { it.element }
//        return MvResolveCache
//            .getInstance(element.project)
//            .resolveWithCaching(element, ResolveCacheDependency.LOCAL_AND_RUST_STRUCTURE, Resolver)
//            .orEmpty()
    }

    override fun handleElementRename(newName: String): PsiElement {
        if (element.parent !is MvPatField) return super.handleElementRename(newName)
        val newFieldPat = element.project.psiFactory.fieldPatFull(newName, element.text)
        return element.replace(newFieldPat)
    }
}

fun resolvePatBindingRaw(binding: MvPatBinding, expectedType: Ty? = null): List<ScopeEntry> {
    val resolveVariants =
        collectResolveVariantsAsScopeEntries(binding.referenceName) {
            val filteringProcessor = filterEnumVariantsByExpectedType(expectedType, it)
            processPatBindingResolveVariants(
                binding,
                false,
                filteringProcessor
            )
//            if (processPatBindingResolveVariants(binding, false, it))
//                return@collectResolveVariantsAsScopeEntries
//            if (expectedType != null) {
//                // expected type is available, can be used to resolve into enum variants
//                val enumItem = (expectedType as? TyAdt)?.item as? MvEnum
//                if (enumItem != null) {
//                    if (it.processAll(TYPES, enumItem.variants)) return@collectResolveVariantsAsScopeEntries
//                }
            }
    return resolveVariants
//    val ctx = ResolutionContext(path, false)
//    val kind = path.pathKind()
//    val resolveVariants =
//        collectResolveVariantsAsScopeEntries(path.referenceName) {
//            if (processPathResolveVariants(ctx, kind, it)) return@collectResolveVariantsAsScopeEntries
//            if (expectedType != null) {
//                // expected type is available, can be used to resolve into enum variants
//                val enumItem = (expectedType as? TyAdt)?.item as? MvEnum
//                if (enumItem != null) {
//                    if (it.processAll(kind.ns, enumItem.variants)) return@collectResolveVariantsAsScopeEntries
//                }
//            }
//        }
//    return resolveVariants
}
