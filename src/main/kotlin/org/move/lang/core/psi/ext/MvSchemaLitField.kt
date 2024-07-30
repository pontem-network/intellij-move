package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import org.move.lang.MvElementTypes
import org.move.lang.core.psi.*
import org.move.lang.core.resolve.*
import org.move.lang.core.resolve.ref.MvPolyVariantReference
import org.move.lang.core.resolve.ref.MvPolyVariantReferenceCached
import org.move.lang.core.resolve.ref.Namespace

val MvSchemaLitField.isShorthand get() = !hasChild(MvElementTypes.COLON)

val MvSchemaLitField.schemaLit: MvSchemaLit? get() = ancestorStrict(stopAt = MvSpecCodeBlock::class.java)

inline fun <reified T: MvElement> MvSchemaLitField.resolveToElement(): T? =
    reference.multiResolve().filterIsInstance<T>().singleOrNull()

fun MvSchemaLitField.resolveToDeclaration(): MvSchemaFieldStmt? = resolveToElement()
fun MvSchemaLitField.resolveToBinding(): MvBindingPat? = resolveToElement()

class MvSchemaFieldReferenceImpl(
    element: MvSchemaLitField
): MvPolyVariantReferenceCached<MvSchemaLitField>(element) {
    override fun multiResolveInner(): List<MvNamedElement> = collectSchemaLitFieldResolveVariants(element)
}

class MvSchemaFieldShorthandReferenceImpl(
    element: MvSchemaLitField
): MvPolyVariantReferenceCached<MvSchemaLitField>(element) {
    override fun multiResolveInner(): List<MvNamedElement> {
        return listOf(
            collectSchemaLitFieldResolveVariants(element),
            resolveLocalItem(element, setOf(Namespace.NAME))
        ).flatten()
    }
}

abstract class MvSchemaLitFieldMixin(node: ASTNode): MvElementImpl(node),
                                                     MvSchemaLitField {
    override fun getReference(): MvPolyVariantReference {
        if (this.isShorthand) {
            return MvSchemaFieldShorthandReferenceImpl(this)
        } else {
            return MvSchemaFieldReferenceImpl(this)
        }
    }
}

fun processSchemaLitFieldResolveVariants(
    literalField: MvSchemaLitField,
    processor: RsResolveProcessor
): Boolean {
    val schemaLit = literalField.schemaLit ?: return false
    val schema = schemaLit.path.maybeSchema ?: return false
    return schema.fieldBindings
        .any { field ->
            processor.process(SimpleScopeEntry(field.name, field, setOf(Namespace.NAME)))
        }
}

fun collectSchemaLitFieldResolveVariants(literalField: MvSchemaLitField): List<MvNamedElement> {
    val referenceName = literalField.referenceName
    val items = mutableListOf<MvNamedElement>()
    processSchemaLitFieldResolveVariants(literalField, createProcessor { e ->
        if (e.name == referenceName) {
            items.add(e.element)
        }
    })
    return items
}
