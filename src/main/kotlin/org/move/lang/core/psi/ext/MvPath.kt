package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import org.move.cli.settings.debugErrorOrFallback
import org.move.ide.annotator.BUILTIN_TYPE_IDENTIFIERS
import org.move.ide.annotator.PRIMITIVE_TYPE_IDENTIFIERS
import org.move.ide.annotator.SPEC_ONLY_PRIMITIVE_TYPES
import org.move.ide.inspections.imports.BasePathType
import org.move.ide.inspections.imports.basePathType
import org.move.lang.core.psi.*
import org.move.lang.core.resolve.ref.MvPath2Reference
import org.move.lang.core.resolve.ref.MvReferenceElement
import org.move.lang.core.resolve.ref.Namespace
import org.move.lang.core.resolve2.ref.Path2ReferenceImpl
import java.util.*

/** For `Foo::bar::baz::quux` path returns `Foo` */
tailrec fun <T: MvPath> T.basePath(): T {
    @Suppress("UNCHECKED_CAST")
    val qualifier = path as T?
    return if (qualifier === null) this else qualifier.basePath()
}

/** For `Foo::bar` in `Foo::bar::baz::quux` returns `Foo::bar::baz::quux` */
tailrec fun MvPath.rootPath(): MvPath {
    // Use `parent` instead of `context` because of better performance.
    // Assume nobody set a context for a part of a path
    val parent = parent
    return if (parent is MvPath) parent.rootPath() else this
}

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

//val MvPath.nullModuleRef: Boolean
//    get() =
//        identifier != null && this.moduleRef == null

val MvPath.maybeStruct get() = reference?.resolveFollowingAliases() as? MvStruct

val MvPath.maybeSchema get() = reference?.resolveFollowingAliases() as? MvSchema

fun MvPath.allowedNamespaces(): Set<Namespace> {
    val parent = this.parent
//    return when (parent) {
//        is MvPath,
//        is MvPathType -> TYPES
//        else -> VALUES
//    }
    return if (parent is MvPath) EnumSet.of(Namespace.MODULE) else rootNamespace(this)
//    return when {
//        parent is MvSchemaLit || parent is MvSchemaRef -> setOf(Namespace.SCHEMA)
//        parent is MvPathType -> setOf(Namespace.TYPE)
//        parent is MvCallExpr -> setOf(Namespace.FUNCTION)
//        parent is MvRefExpr && parent.isAbortCodeConst() -> setOf(Namespace.CONST)
//        parent is MvRefExpr -> setOf(Namespace.NAME)
////            parent is MvRefExpr && this.nullModuleRef -> setOf(Namespace.NAME)
////            parent is MvRefExpr && !this.nullModuleRef -> setOf(Namespace.NAME, Namespace.MODULE)
//        // TODO: it's own namespace?
//        parent is MvStructLitExpr || parent is MvStructPat -> setOf(Namespace.NAME)
//        parent is MvAccessSpecifier -> setOf(Namespace.TYPE)
//        parent is MvAddressSpecifierArg -> setOf(Namespace.FUNCTION)
//        parent is MvAddressSpecifierCallParam -> setOf(Namespace.NAME)
//        else -> debugErrorOrFallback(
//            "Cannot build path namespaces: unhandled parent type ${parent.elementType}",
//            setOf(Namespace.NAME)
//        )
//    }
}

private fun rootNamespace(rootPath: MvPath): Set<Namespace> {
    val parent = rootPath.parent
    check(parent !is MvPath)
    return when {
        parent is MvSchemaLit || parent is MvSchemaRef -> EnumSet.of(Namespace.SCHEMA)
        parent is MvPathType -> EnumSet.of(Namespace.TYPE)
        parent is MvCallExpr -> EnumSet.of(Namespace.FUNCTION)
//        parent is MvRefExpr && parent.isAbortCodeConst() -> EnumSet.of(Namespace.CONST)
        parent is MvRefExpr -> EnumSet.of(Namespace.NAME)
//        parent is MvRefExpr -> EnumSet.of(Namespace.NAME, Namespace.CONST)
        parent is MvStructLitExpr || parent is MvStructPat -> EnumSet.of(Namespace.TYPE)
//        parent is MvStructLitExpr || parent is MvStructPat -> EnumSet.of(Namespace.NAME)
        parent is MvAccessSpecifier -> EnumSet.of(Namespace.TYPE)
        parent is MvAddressSpecifierArg -> EnumSet.of(Namespace.FUNCTION)
        parent is MvAddressSpecifierCallParam -> EnumSet.of(Namespace.NAME)
        parent is MvUseSpeck -> Namespace.all()
        else -> debugErrorOrFallback(
            "Cannot build path namespaces: unhandled parent type ${parent.elementType}",
            EnumSet.of(Namespace.NAME)
        )
    }
}

val MvPath.qualifier: MvPath?
    get() {
        path?.let { return it }
        var ctx = context
        while (ctx is MvPath) {
            ctx = ctx.context
        }
        // returns the base qualifier, if it's inside the MvUseGroup
        return (ctx as? MvUseSpeck)?.qualifier
    }

abstract class MvPathMixin(node: ASTNode): MvElementImpl(node), MvPath {

    override fun getReference(): MvPath2Reference? = Path2ReferenceImpl(this)
}

fun MvPath.importCandidateNamespaces(): Set<Namespace> {
    val parent = this.parent
    return when (parent) {
        is MvPathType -> setOf(Namespace.TYPE)
        is MvSchemaLit, is MvSchemaRef -> setOf(Namespace.SCHEMA)
        else -> {
            val baseBaseType = this.basePathType()
            when (baseBaseType) {
                is BasePathType.Module -> EnumSet.of(Namespace.MODULE)
                else -> EnumSet.of(Namespace.NAME, Namespace.FUNCTION)
            }
        }
    }
}

val MvPath.moduleRef: MvModuleRef? get() = error("unimplemented")

val MvPath.hasColonColon: Boolean get() = colonColon != null