package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import org.move.lang.MvElementTypes
import org.move.lang.core.psi.*
import org.move.lang.core.resolve.RsResolveProcessor
import org.move.lang.core.resolve.SimpleScopeEntry
import org.move.lang.core.resolve.collectResolveVariants
import org.move.lang.core.resolve.ref.MvMandatoryReferenceElement
import org.move.lang.core.resolve.ref.MvPolyVariantReference
import org.move.lang.core.resolve.ref.MvPolyVariantReferenceCached
import org.move.lang.core.resolve.ref.Namespace
import org.move.lang.core.resolve2.resolveBindingForFieldShorthand

val MvStructLitField.structLitExpr: MvStructLitExpr
    get() = ancestorStrict()!!

val MvStructLitField.isShorthand: Boolean
    get() = !hasChild(MvElementTypes.COLON)

inline fun <reified T: MvElement> MvStructLitField.resolveToElement(): T? =
    reference.multiResolve().filterIsInstance<T>().singleOrNull()

fun MvStructLitField.resolveToDeclaration(): MvStructField? = resolveToElement()
fun MvStructLitField.resolveToBinding(): MvBindingPat? = resolveToElement()

interface MvStructRefField: MvMandatoryReferenceElement

abstract class MvStructLitFieldMixin(node: ASTNode): MvElementImpl(node),
                                                     MvStructLitField {
    override fun getReference(): MvPolyVariantReference =
        MvStructRefFieldReferenceImpl(this, shorthand = this.isShorthand)
//        if (this.isShorthand) {
//            return MvStructLitShorthandFieldReferenceImpl(this)
//        } else {
//            return MvStructRefFieldReferenceImpl(this)
//        }
}

class MvStructRefFieldReferenceImpl(
    element: MvStructRefField,
    var shorthand: Boolean,
): MvPolyVariantReferenceCached<MvStructRefField>(element) {

    override fun multiResolveInner(): List<MvNamedElement> {
        val referenceName = element.referenceName ?: return emptyList()
        var variants = collectResolveVariants(referenceName) {
            processStructRefFieldResolveVariants(element, it)
        }
//        var variants = resolveIntoStructField(element)
        if (shorthand) {
            variants += resolveBindingForFieldShorthand(element)
//            variants += resolveLocalItem(element, setOf(Namespace.NAME))
        }
        return variants
//        return listOf(
//            resolveIntoStructField(element),
//            resolveLocalItem(element, setOf(Namespace.NAME))
//        ).flatten()
    }
}

fun processStructRefFieldResolveVariants(
    fieldRef: MvStructRefField,
    processor: RsResolveProcessor
): Boolean {
    val structItem = fieldRef.maybeStruct ?: return false
    return structItem.fields
        .any { field ->
            processor.process(SimpleScopeEntry(field.name, field, setOf(Namespace.NAME)))
        }
}

fun resolveIntoStructField(element: MvStructRefField): List<MvNamedElement> {
    val structItem = element.maybeStruct ?: return emptyList()
    val referenceName = element.referenceName
    return structItem.fields
        .filter { it.name == referenceName }
}

private val MvStructRefField.maybeStruct: MvStruct?
    get() {
        return when (this) {
            is MvStructPatField -> this.structPat.structItem
            is MvStructLitField -> this.structLitExpr.path.maybeStruct
            else -> null
        }
    }



