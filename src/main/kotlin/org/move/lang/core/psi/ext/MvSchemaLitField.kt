package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import org.move.lang.MvElementTypes
import org.move.lang.core.psi.*
import org.move.lang.core.resolve.RsResolveProcessor
import org.move.lang.core.resolve.SimpleScopeEntry
import org.move.lang.core.resolve.collectResolveVariants
import org.move.lang.core.resolve.ref.MvPolyVariantReference
import org.move.lang.core.resolve.ref.MvPolyVariantReferenceCached
import org.move.lang.core.resolve.ref.Namespace
import org.move.lang.core.resolve.resolveBindingForFieldShorthand

val MvSchemaLitField.isShorthand get() = !hasChild(MvElementTypes.COLON)

val MvSchemaLitField.schemaLit: MvSchemaLit? get() = ancestorStrict(stopAt = MvSpecCodeBlock::class.java)

inline fun <reified T: MvElement> MvSchemaLitField.resolveToElement(): T? =
    reference.multiResolve().filterIsInstance<T>().singleOrNull()

fun MvSchemaLitField.resolveToDeclaration(): MvSchemaFieldStmt? = resolveToElement()
fun MvSchemaLitField.resolveToBinding(): MvPatBinding? = resolveToElement()

//class MvSchemaFieldReferenceImpl(
//    element: MvSchemaLitField
//): MvPolyVariantReferenceCached<MvSchemaLitField>(element) {
//    override fun multiResolveInner(): List<MvNamedElement> = collectSchemaLitFieldResolveVariants(element)
//}

//class MvSchemaFieldShorthandReferenceImpl(
//    element: MvSchemaLitField
//): MvPolyVariantReferenceCached<MvSchemaLitField>(element) {
//    override fun multiResolveInner(): List<MvNamedElement> {
//        return listOf(
//            collectSchemaLitFieldResolveVariants(element),
//            resolveLocalItem(element, setOf(Namespace.NAME))
//        ).flatten()
//    }
//}

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
        var variants = collectResolveVariants(element.referenceName) {
            processSchemaLitFieldResolveVariants(element, it)
        }
        if (shorthand) {
            variants += resolveBindingForFieldShorthand(element)
//            variants += resolveLocalItem(element, setOf(Namespace.NAME))
        }
        return variants
    }
}

fun processSchemaLitFieldResolveVariants(
    literalField: MvSchemaLitField,
    processor: RsResolveProcessor
): Boolean {
    val schemaLit = literalField.schemaLit ?: return false
    val schema = schemaLit.path.maybeSchema ?: return false
    return schema.fieldsAsBindings
        .any { field ->
            processor.process(SimpleScopeEntry(field.name, field, setOf(Namespace.NAME)))
        }
}

