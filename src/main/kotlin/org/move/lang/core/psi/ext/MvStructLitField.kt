package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import org.move.lang.core.psi.*
import org.move.lang.core.resolve.ref.MvMandatoryReferenceElement
import org.move.lang.core.resolve.ref.MvPolyVariantReference
import org.move.lang.core.resolve.ref.MvStructLitFieldReferenceImpl

val MvStructLitField.parentStructLitExpr: MvStructLitExpr
    get() = ancestorStrict()!!

//inline fun <reified T: MvElement> MvStructLitField.resolveToElement(): T? =
//    reference.multiResolve().filterIsInstance<T>().singleOrNull()

//fun MvStructLitField.resolveToDeclaration(): MvNamedFieldDecl? = resolveToElement()
//fun MvStructLitField.resolveToBinding(): MvPatBinding? = resolveToElement()

/**
 * ```
 * struct S {
 *     foo: i32,
 *     bar: i32,
 * }
 * fn main() {
 *     let foo = 1;
 *     let s = S {
 *         foo,   // isShorthand = true
 *         bar: 1 // isShorthand = false
 *     };
 * }
 * ```
 */
val MvStructLitField.isShorthand: Boolean get() = colon == null

interface MvFieldReferenceElement: MvMandatoryReferenceElement

abstract class MvStructLitFieldMixin(node: ASTNode): MvElementImpl(node),
                                                     MvStructLitField {

    override fun getReference(): MvPolyVariantReference = MvStructLitFieldReferenceImpl(this)
}


//class MvFieldReferenceImpl(
//    element: MvFieldReferenceElement,
//    var shorthand: Boolean,
//): MvPolyVariantReferenceCached<MvFieldReferenceElement>(element) {
//
//    override fun multiResolveInner(): List<MvNamedElement> {
//        val referenceName = element.referenceName
//        var variants = collectResolveVariants(referenceName) {
//            processStructRefFieldResolveVariants(element, it)
//        }
//        if (shorthand) {
//            variants += resolveBindingForFieldShorthand(element)
//        }
//        return variants
//    }
//}

//fun processStructRefFieldResolveVariants(
//    fieldRef: MvFieldReferenceElement,
//    processor: RsResolveProcessor
//): Boolean {
//    val fieldsOwnerItem = fieldRef.fieldOwner ?: return false
//    return fieldsOwnerItem.fields
//        .any { field ->
//            processor.process(SimpleScopeEntry(field.name, field, setOf(Namespace.NAME)))
//        }
//}

//private val MvFieldReferenceElement.fieldOwner: MvFieldsOwner?
//    get() {
//        return when (this) {
//            is MvPatField -> {
//                this.patStruct.path.reference?.resolveFollowingAliases() as? MvFieldsOwner
//            }
//            is MvStructLitField -> {
//                this.structLitExpr.path.reference?.resolveFollowingAliases() as? MvFieldsOwner
//            }
//            else -> null
//        }
//    }



