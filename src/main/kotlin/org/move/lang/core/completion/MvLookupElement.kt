package org.move.lang.core.completion

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementDecorator
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.inferBindingTy
import org.move.lang.core.types.infer.*
import org.move.lang.core.types.ty.Ty
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
    val ctx = InferenceContext(context.itemVis.isMsl)
    var props = LookupElementProperties()
    val expectedTy = context.expectedTy
    if (expectedTy != null) {
        val ty = element.asTy(ctx)
        props = props.copy(
            isReturnTypeConformsToExpectedType = isCompatible(context.expectedTy, ty, ctx.msl)
        )
    }
    return props
}

private fun MvNamedElement.asTy(ctx: InferenceContext): Ty {
    val msl = false
    val itemContext = this.itemContextOwner?.itemContext(msl) ?: project.itemContext(msl)
    return when (this) {
//        is RsFieldDecl -> typeReference?.type
        is MvFunction -> this.returnTypeTy(itemContext)
        is MvStruct -> {
            itemContext.getItemTy(this)
        }
        is MvConst -> itemContext.getConstTy(this)
        is MvBindingPat -> this.inferBindingTy(ctx, itemContext)
        else -> TyUnknown
    }
}
