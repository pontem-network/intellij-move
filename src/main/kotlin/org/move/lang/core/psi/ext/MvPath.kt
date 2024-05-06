package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import org.move.cli.settings.debugErrorOrFallback
import org.move.ide.annotator.BUILTIN_TYPE_IDENTIFIERS
import org.move.ide.annotator.PRIMITIVE_TYPE_IDENTIFIERS
import org.move.ide.annotator.SPEC_ONLY_PRIMITIVE_TYPES
import org.move.lang.core.psi.*
import org.move.lang.core.resolve.ref.MvPathReference
import org.move.lang.core.resolve.ref.MvPathReferenceImpl
import org.move.lang.core.resolve.ref.MvReferenceElement
import org.move.lang.core.resolve.ref.Namespace

fun MvPath.isPrimitiveType(): Boolean =
    this.parent is MvPathType
            && this.referenceName in PRIMITIVE_TYPE_IDENTIFIERS.union(BUILTIN_TYPE_IDENTIFIERS)

//fun MvPath.isAttrItem(): Boolean {
//    val attrItem = this.ancestorStrict<MvAttrItem>() ?: return false
//    return attrItem.name == "expected_failure"
//}

fun MvPath.isSpecPrimitiveType(): Boolean =
    this.parent is MvPathType
            && this.referenceName in PRIMITIVE_TYPE_IDENTIFIERS
        .union(BUILTIN_TYPE_IDENTIFIERS)
        .union(SPEC_ONLY_PRIMITIVE_TYPES)

val MvPath.isUpdateFieldArg2: Boolean
    get() {
        if (!this.isMslScope) return false
        val ind = this
            .ancestorStrict<MvCallExpr>()
            ?.let { if (it.path.textMatches("update_field")) it else null }
            ?.let {
                val expr = this.ancestorStrict<MvRefExpr>() ?: return@let -1
                it.argumentExprs.indexOf(expr)
            }
        return ind == 1
    }

val MvPath.identifierName: String? get() = identifier?.text

val MvPath.nullModuleRef: Boolean
    get() =
        identifier != null && this.moduleRef == null

val MvPath.isQualPath: Boolean get() = !this.nullModuleRef

val MvPath.maybeStruct get() = reference?.resolveWithAliases() as? MvStruct

val MvPath.maybeSchema get() = reference?.resolveWithAliases() as? MvSchema

fun MvPath.namespaces(): Set<Namespace> {
    val parent = this.parent
    return when {
        parent is MvSchemaLit || parent is MvSchemaRef -> setOf(Namespace.SCHEMA)
        parent is MvPathType -> setOf(Namespace.TYPE)
        parent is MvCallExpr -> setOf(Namespace.FUNCTION)
        parent is MvRefExpr && parent.isAbortCodeConst() -> setOf(Namespace.CONST)
        parent is MvRefExpr -> setOf(Namespace.NAME)
//            parent is MvRefExpr && this.nullModuleRef -> setOf(Namespace.NAME)
//            parent is MvRefExpr && !this.nullModuleRef -> setOf(Namespace.NAME, Namespace.MODULE)
        // TODO: it's own namespace?
        parent is MvStructLitExpr || parent is MvStructPat -> setOf(Namespace.NAME)
        else -> debugErrorOrFallback(
            "Unhandled path parent ${parent.elementType}",
            setOf(Namespace.NAME)
        )
    }
}

abstract class MvPathMixin(node: ASTNode) : MvElementImpl(node), MvPath {

    override fun getReference(): MvPathReference? = MvPathReferenceImpl(this)
}

fun MvReferenceElement.importCandidateNamespaces(): Set<Namespace> {
    val parent = this.parent
    return when (parent) {
        is MvPathType -> setOf(Namespace.TYPE)
        is MvSchemaLit, is MvSchemaRef -> setOf(Namespace.SCHEMA)
        else ->
            when (this) {
                is MvModuleRef -> setOf(Namespace.MODULE)
                is MvPath -> setOf(Namespace.NAME, Namespace.FUNCTION)
                else -> Namespace.all()
            }
    }
}
