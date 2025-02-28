package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import org.move.lang.MvElementTypes
import org.move.lang.core.psi.*
import org.move.lang.core.resolve.ScopeEntry
import org.move.lang.core.resolve.asEntries
import org.move.lang.core.resolve.filterByName
import org.move.lang.core.resolve.ref.MvPolyVariantReference
import org.move.lang.core.resolve.ref.MvPolyVariantReferenceCached
import org.move.lang.core.resolve.resolveBindingForFieldShorthand

val MvSchemaLitField.isShorthand get() = !hasChild(MvElementTypes.COLON)

val MvSchemaLitField.schemaLit: MvSchemaLit? get() = ancestorStrict(stopAt = MvSpecCodeBlock::class.java)

inline fun <reified T: MvElement> MvSchemaLitField.resolveToElement(): T? =
    reference.multiResolve().filterIsInstance<T>().singleOrNull()

fun MvSchemaLitField.resolveToDeclaration(): MvSchemaFieldStmt? = resolveToElement()
fun MvSchemaLitField.resolveToBinding(): MvPatBinding? = resolveToElement()

abstract class MvSchemaLitFieldMixin(node: ASTNode): MvElementImpl(node),
                                                     MvSchemaLitField {
    override fun getReference(): MvPolyVariantReference =
        MvSchemaLitFieldReferenceImpl(this, shorthand = this.isShorthand)
}

class MvSchemaLitFieldReferenceImpl(
    element: MvSchemaLitField,
    val shorthand: Boolean,
): MvPolyVariantReferenceCached<MvSchemaLitField>(element) {
    override fun multiResolveInner(): List<MvNamedElement> {
        val variants = getSchemaLitFieldResolveVariants(element)
            .filterByName(element.referenceName)
            .toMutableList()
        if (shorthand) {
            variants += resolveBindingForFieldShorthand(element)
        }
        return variants.map { it.element }
    }
}

fun getSchemaLitFieldResolveVariants(literalField: MvSchemaLitField): List<ScopeEntry> {
    val schema = literalField.schemaLit?.path?.maybeSchema
        ?: return emptyList()
    return schema.fieldsAsBindings.asEntries()
}

