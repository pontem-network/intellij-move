package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import org.move.ide.annotator.BUILTIN_TYPE_IDENTIFIERS
import org.move.ide.annotator.PRIMITIVE_TYPE_IDENTIFIERS
import org.move.ide.annotator.SPEC_ONLY_PRIMITIVE_TYPES
import org.move.lang.MvElementTypes
import org.move.lang.core.psi.*
import org.move.lang.core.resolve.MvReferenceElement
import org.move.lang.core.resolve.ref.MvPathReference
import org.move.lang.core.resolve.ref.MvPathReferenceImpl
import org.move.lang.core.resolve.ref.Namespace

fun MvPath.isPrimitiveType(): Boolean =
    this.parent is MvPathType
            && this.referenceName in PRIMITIVE_TYPE_IDENTIFIERS.union(BUILTIN_TYPE_IDENTIFIERS)

fun MvPath.isSpecPrimitiveType(): Boolean =
    this.parent is MvPathType
            && this.referenceName in PRIMITIVE_TYPE_IDENTIFIERS
        .union(BUILTIN_TYPE_IDENTIFIERS)
        .union(SPEC_ONLY_PRIMITIVE_TYPES)

val MvPath.isResult: Boolean get() = this.textMatches("result") || this.text.startsWith("result_")

val MvPath.isUpdateFieldArg2: Boolean
    get() {
        if (!this.isMsl()) return false
        val ind = this
            .ancestorStrict<MvCallExpr>()
            ?.let { if (it.path.textMatches("update_field")) it else null }
            ?.let {
                val expr = this.ancestorStrict<MvRefExpr>() ?: return@let -1
                it.callArgumentExprs.indexOf(expr)
            }
        return ind == 1
    }

val MvPath.identifierName: String? get() = identifier?.text

val MvPath.colonColon get() = this.findFirstChildByType(MvElementTypes.COLON_COLON)

val MvPath.isLocal: Boolean
    get() =
        identifier != null && this.moduleRef == null

val MvPath.isQual: Boolean get() = !this.isLocal

val MvPath.typeArguments: List<MvTypeArgument>
    get() = typeArgumentList?.typeArgumentList.orEmpty()

val MvPath.maybeStruct get() = reference?.resolve() as? MvStruct

val MvPath.maybeSchema get() = reference?.resolve() as? MvSchema

abstract class MvPathMixin(node: ASTNode) : MvElementImpl(node), MvPath {

    override fun getReference(): MvPathReference? {
        val namespaces = when (this.parent) {
            is MvSchemaLit, is MvSchemaRef -> setOf(Namespace.SCHEMA)
            is MvPathType -> setOf(Namespace.TYPE)
            else -> {
                if (this.isLocal) {
                    setOf(Namespace.NAME, Namespace.MODULE)
                } else {
                    setOf(Namespace.NAME)
                }
            }
        }
        return MvPathReferenceImpl(this, namespaces)
    }
}

fun MvReferenceElement.namespaces(): Set<Namespace> {
    val parent = this.parent
    return when (parent) {
        is MvPathType -> setOf(Namespace.TYPE)
        is MvSchemaLit, is MvSchemaRef -> setOf(Namespace.SCHEMA)
        else ->
            when (this) {
                is MvModuleRef -> setOf(Namespace.MODULE)
                is MvPath -> setOf(Namespace.NAME)
                else -> Namespace.allNames()
            }
    }
}
