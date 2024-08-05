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

fun MvStructLitField.resolveToDeclaration(): MvNamedFieldDecl? = resolveToElement()
fun MvStructLitField.resolveToBinding(): MvBindingPat? = resolveToElement()

interface MvStructRefField: MvMandatoryReferenceElement

abstract class MvStructLitFieldMixin(node: ASTNode): MvElementImpl(node),
                                                     MvStructLitField {
    override fun getReference(): MvPolyVariantReference =
        MvStructRefFieldReferenceImpl(this, shorthand = this.isShorthand)
}

class MvStructRefFieldReferenceImpl(
    element: MvStructRefField,
    var shorthand: Boolean,
): MvPolyVariantReferenceCached<MvStructRefField>(element) {

    override fun multiResolveInner(): List<MvNamedElement> {
        val referenceName = element.referenceName
        var variants = collectResolveVariants(referenceName) {
            processStructRefFieldResolveVariants(element, it)
        }
        if (shorthand) {
            variants += resolveBindingForFieldShorthand(element)
        }
        return variants
    }
}

fun processStructRefFieldResolveVariants(
    fieldRef: MvStructRefField,
    processor: RsResolveProcessor
): Boolean {
    val fieldsOwnerItem = fieldRef.fieldOwner ?: return false
    return fieldsOwnerItem.fields
        .any { field ->
            processor.process(SimpleScopeEntry(field.name, field, setOf(Namespace.NAME)))
        }
}

private val MvStructRefField.fieldOwner: MvFieldsOwner?
    get() {
        return when (this) {
            is MvStructPatField -> {
                this.structPat.path.reference?.resolveFollowingAliases() as? MvFieldsOwner
            }
            is MvStructLitField -> {
                this.structLitExpr.path.reference?.resolveFollowingAliases() as? MvFieldsOwner
            }
            else -> null
        }
    }



