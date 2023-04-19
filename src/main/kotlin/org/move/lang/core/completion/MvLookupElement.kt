package org.move.lang.core.completion

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementDecorator
import org.move.lang.core.psi.*
import org.move.lang.core.types.infer.compatAbilities
import org.move.lang.core.types.infer.inference
import org.move.lang.core.types.infer.isCompatible
import org.move.lang.core.types.infer.loweredType
import org.move.lang.core.types.ty.TyUnknown

fun LookupElement.toMvLookupElement(properties: LookupElementProperties): MvLookupElement =
    MvLookupElement(this, properties)

class MvLookupElement(
    delegate: LookupElement,
    val props: LookupElementProperties
) : LookupElementDecorator<LookupElement>(delegate) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as MvLookupElement

        if (props != other.props) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + props.hashCode()
        return result
    }
}

data class LookupElementProperties(
    /**
     * `true` if after insertion of the lookup element it will form an expression with a type
     * that conforms to the expected type of that expression.
     *
     * ```
     * fn foo() -> String { ... } // isReturnTypeConformsToExpectedType = true
     * fn bar() -> i32 { ... }    // isReturnTypeConformsToExpectedType = false
     * fn main() {
     *     let a: String = // <-- complete here
     * }
     * ```
     */
    val isReturnTypeConformsToExpectedType: Boolean = false,

    val isCompatibleWithContext: Boolean = false,

    val typeHasAllRequiredAbilities: Boolean = false,
)

fun lookupProperties(element: MvNamedElement, context: CompletionContext): LookupElementProperties {
    var props = LookupElementProperties()
    val msl = context.itemVis.isMsl
    val expectedTy = context.expectedTy
    if (expectedTy != null) {
//        val itemContext = element.itemContextOwner?.itemContext(msl) ?: element.project.itemContext(msl)
//        val ctx = InferenceContext(msl, itemContext)
//        val typeContext =
        val itemTy = when (element) {
//        is RsFieldDecl -> typeReference?.type
            is MvFunctionLike -> element.declaredType(msl).retType
            is MvStruct -> element.declaredType(msl)
//            is MvFunction -> {
//                element.declaredType(msl)
////                val itemContext = element.outerItemContext(msl)
////                (itemContext.getItemTy(element) as? TyFunction)?.retType ?: TyUnknown
//            }
//            is MvStruct -> {
//                TyStruct2.valueOf(element)
////                element.outerItemContext(msl).getItemTy(element)
//            }
            is MvConst -> element.type?.loweredType(msl) ?: TyUnknown
            is MvBindingPat -> {
                val inference = element.inference(msl)
                inference?.getPatType(element) ?: TyUnknown
            }
            else -> TyUnknown
        }
        // it is required for the TyInfer.TyVar to always have a different underlying unification table
//        val ty = if (expectedTy is TyInfer.TyVar) TyInfer.TyVar(expectedTy.origin) else expectedTy
        val isCompat = isCompatible(expectedTy, itemTy, msl) && compatAbilities(expectedTy, itemTy, msl)
        props = props.copy(
            isReturnTypeConformsToExpectedType = isCompat
        )
    }
    return props
}

//private fun isCompatibleTypes(actualTy: Ty?, expectedTy: Ty?, msl: Boolean): Boolean {
//    if (actualTy == null || expectedTy == null) return false
//    if (
//        actualTy is TyUnknown || expectedTy is TyUnknown ||
//        actualTy is TyNever || expectedTy is TyNever ||
//        actualTy is TyTypeParameter || expectedTy is TyTypeParameter
//    ) return false
//
//    // Replace `TyUnknown` and `TyTypeParameter` with `TyNever` to ignore them when combining types
//    val folder = object : TypeFolder() {
//        override fun fold(ty: Ty): Ty = when (ty) {
//            is TyUnknown -> TyNever
//            is TyTypeParameter -> TyNever
//            else -> ty.innerFoldWith(this)
//        }
//    }
//
//    // TODO coerce
//    val ty1 = actualTy.foldWith(folder)
//    val ty2 = expectedTy.foldWith(folder)
//    return ctx.combineTypesNoVars(ty1, ty2).isOk
////    return if (expectedTy.coercable) {
////        lookup.ctx.tryCoerce(ty1, ty2)
////    } else {
////        lookup.ctx.combineTypesNoVars(ty1, ty2)
////    }.isOk
//}


//private fun MvNamedElement.asTy(ctx: InferenceContext): Ty {
//    val msl = false
//    val itemContext = this.itemContextOwner?.itemContext(msl) ?: project.itemContext(msl)
//    return when (this) {
////        is RsFieldDecl -> typeReference?.type
//        is MvFunction -> this.returnTypeTy(itemContext)
//        is MvStruct -> {
//            itemContext.getItemTy(this)
//        }
//        is MvConst -> itemContext.getConstTy(this)
//        is MvBindingPat -> this.inferBindingTy(ctx, itemContext)
//        else -> TyUnknown
//    }
//}
