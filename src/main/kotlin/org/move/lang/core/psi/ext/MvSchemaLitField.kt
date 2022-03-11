package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.move.lang.MvElementTypes
import org.move.lang.core.psi.*
import org.move.lang.core.resolve.ref.MvReference
import org.move.lang.core.resolve.ref.MvReferenceCached
import org.move.lang.core.resolve.ref.Namespace
import org.move.lang.core.resolve.resolveItem

val MvSchemaLitField.isShorthand get() = !hasChild(MvElementTypes.COLON)

val MvSchemaLitField.schemaLit: MvSchemaLit? get() = ancestorStrict(MvSpecBlock::class.java)

class MvSchemaFieldReferenceImpl(
    element: MvSchemaLitField
) : MvReferenceCached<MvSchemaLitField>(element) {
    override fun resolveInner(): List<MvNamedElement> {
        return resolveItem(element, Namespace.SCHEMA_FIELD)
    }
}

class MvSchemaFieldShorthandReferenceImpl(
    element: MvSchemaLitField
) : MvReferenceCached<MvSchemaLitField>(element) {
    override fun resolveInner(): List<MvNamedElement> {
        return listOf(
            resolveItem(element, Namespace.SCHEMA_FIELD),
            resolveItem(element, Namespace.NAME)
        ).flatten()
    }

    override fun handleElementRename(newName: String): PsiElement {
        val psiFactory = element.project.psiFactory
        val newField = psiFactory.createSchemaLitField(newName, element.referenceName)
        element.replace(newField)
        return element
    }
}

abstract class MvSchemaLitFieldMixin(node: ASTNode) : MvElementImpl(node),
                                                      MvSchemaLitField {
    override fun getReference(): MvReference {
        if (this.isShorthand) {
            return MvSchemaFieldShorthandReferenceImpl(this)
        } else {
            return MvSchemaFieldReferenceImpl(this)
        }
    }
}
