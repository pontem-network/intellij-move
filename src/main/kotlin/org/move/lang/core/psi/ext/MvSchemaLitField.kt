package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import org.move.lang.MvElementTypes
import org.move.lang.core.psi.*
import org.move.lang.core.resolve.ref.MvPolyVariantReference
import org.move.lang.core.resolve.ref.MvPolyVariantReferenceCached
import org.move.lang.core.resolve.ref.Namespace
import org.move.lang.core.resolve.resolveLocalItem

val MvSchemaLitField.isShorthand get() = !hasChild(MvElementTypes.COLON)

val MvSchemaLitField.schemaLit: MvSchemaLit? get() = ancestorStrict(stopAt = MvSpecCodeBlock::class.java)

inline fun <reified T : MvElement> MvSchemaLitField.resolveToElement(): T? =
    reference.multiResolve().filterIsInstance<T>().singleOrNull()

fun MvSchemaLitField.resolveToDeclaration(): MvSchemaFieldStmt? = resolveToElement()
fun MvSchemaLitField.resolveToBinding(): MvBindingPat? = resolveToElement()

class MvSchemaFieldReferenceImpl(
    element: MvSchemaLitField
) : MvPolyVariantReferenceCached<MvSchemaLitField>(element) {
    override fun multiResolveInner(): List<MvNamedElement> = resolveIntoSchemaField(element)
}

class MvSchemaFieldShorthandReferenceImpl(
    element: MvSchemaLitField
) : MvPolyVariantReferenceCached<MvSchemaLitField>(element) {
    override fun multiResolveInner(): List<MvNamedElement> {
        return listOf(
            resolveIntoSchemaField(element),
            resolveLocalItem(element, setOf(Namespace.NAME))
        ).flatten()
    }
}

abstract class MvSchemaLitFieldMixin(node: ASTNode) : MvElementImpl(node),
                                                      MvSchemaLitField {
    override fun getReference(): MvPolyVariantReference {
        if (this.isShorthand) {
            return MvSchemaFieldShorthandReferenceImpl(this)
        } else {
            return MvSchemaFieldReferenceImpl(this)
        }
    }
}

private fun resolveIntoSchemaField(element: MvSchemaLitField): List<MvNamedElement> {
    val schemaLit = element.schemaLit ?: return emptyList()
    val schema = schemaLit.path.maybeSchema
    val referenceName = element.referenceName
    return schema
        ?.fieldBindings.orEmpty()
        .filter { it.name == referenceName }
}