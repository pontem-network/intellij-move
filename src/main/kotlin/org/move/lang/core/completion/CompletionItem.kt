package org.move.lang.core.completion

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementDecorator
import org.move.lang.core.psi.*
import org.move.lang.core.types.infer.*
import org.move.lang.core.types.ty.TyAdt
import org.move.lang.core.types.ty.TyUnknown
import org.move.lang.core.types.ty.functionTy

fun LookupElement.toCompletionItem(properties: LookupElementProperties): CompletionItem =
    CompletionItem(this, properties)

class CompletionItem(
    delegate: LookupElement,
    val props: LookupElementProperties
):
    LookupElementDecorator<LookupElement>(delegate) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as CompletionItem

        return props == other.props
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

//    val isCompatibleWithContext: Boolean = false,

//    val typeHasAllRequiredAbilities: Boolean = false,
)

fun getLookupElementProperties(
    element: MvNamedElement,
    applySubst: Substitution,
    context: MvCompletionContext
): LookupElementProperties {
    var props = LookupElementProperties()
    val expectedTy = context.expectedTy
    if (expectedTy != null) {
        val msl = context.msl
        val declaredItemTy =
            when (element) {
                is MvFunctionLike -> element.functionTy(msl).returnType
                is MvStruct -> TyAdt.valueOf(element)
                is MvConst -> element.type?.loweredType(msl) ?: TyUnknown
                is MvPatBinding -> {
                    val inference = element.inference(msl)
                    // sometimes type inference won't be able to catch up with the completion, and this line crashes,
                    // so changing to infallible getPatTypeOrUnknown()
                    inference?.getPatTypeOrUnknown(element) ?: TyUnknown
                }
                is MvNamedFieldDecl -> element.type?.loweredType(msl) ?: TyUnknown
                else -> TyUnknown
            }
        val itemTy = declaredItemTy.substitute(applySubst)

        // NOTE: it is required for the TyInfer.TyVar to always have a different underlying unification table
        val isCompat = isCompatible(expectedTy, itemTy, msl) && compatAbilities(expectedTy, itemTy, msl)
        props = props.copy(
            isReturnTypeConformsToExpectedType = isCompat
        )
    }
    return props
}
