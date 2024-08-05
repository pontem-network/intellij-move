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
import org.move.lang.core.resolve.ref.Namespace
import org.move.lang.core.resolve.ref.Namespace.*
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

val MvPath.length: Int get() {
    var length = 1
    var currentPath = this
    while (currentPath.path != null) {
        currentPath = currentPath.path!!
        length += 1
    }
    return length
}

fun MvPath.isPrimitiveType(): Boolean =
    this.parent is MvPathType
            && this.referenceName in PRIMITIVE_TYPE_IDENTIFIERS.union(BUILTIN_TYPE_IDENTIFIERS)

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

val MvPath.maybeStruct get() = reference?.resolveFollowingAliases() as? MvStruct

val MvPath.maybeSchema get() = reference?.resolveFollowingAliases() as? MvSchema

fun MvPath.allowedNamespaces(): Set<Namespace> {
    val qualifierPath = this.path
    val parentElement = this.parent

    // m::S, S::One
    // ^     ^
    if (parentElement is MvPath && qualifierPath == null) return EnumSet.of(MODULE, TYPE)

    // m::S::One
    // ^
    if (parentElement is MvPath && parentElement.parent is MvPath) return EnumSet.of(MODULE)

    // m::S::One
    //    ^
    if (parentElement is MvPath/* && qualifierPath != null*/) return EnumSet.of(TYPE)

    return when {
        parentElement is MvSchemaLit || parentElement is MvSchemaRef -> EnumSet.of(SCHEMA)
        parentElement is MvPathType -> EnumSet.of(TYPE)
        parentElement is MvCallExpr -> EnumSet.of(FUNCTION)
        parentElement is MvRefExpr && this.hasAncestor<MvAttrItem>() -> EnumSet.of(NAME, MODULE)
        parentElement is MvRefExpr -> EnumSet.of(NAME)
        parentElement is MvStructLitExpr
                || parentElement is MvStructPat
                || parentElement is MvEnumVariantPat -> EnumSet.of(TYPE)
        parentElement is MvAccessSpecifier -> EnumSet.of(TYPE)
        parentElement is MvAddressSpecifierArg -> EnumSet.of(FUNCTION)
        parentElement is MvAddressSpecifierCallParam -> EnumSet.of(NAME)
        parentElement is MvFriendDecl -> EnumSet.of(MODULE)
        parentElement is MvModuleSpec -> EnumSet.of(MODULE)
        parentElement is MvUseSpeck -> Namespace.all()
        else -> debugErrorOrFallback(
            "Cannot build path namespaces: unhandled parent type ${parentElement.elementType}",
            EnumSet.of(NAME)
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
        is MvPathType -> setOf(TYPE)
        is MvSchemaLit, is MvSchemaRef -> setOf(SCHEMA)
        else -> {
            val baseBaseType = this.basePathType()
            when (baseBaseType) {
                is BasePathType.Module -> EnumSet.of(MODULE)
                else -> EnumSet.of(NAME, FUNCTION)
            }
        }
    }
}

val MvPath.hasColonColon: Boolean get() = colonColon != null

val MvPath.useSpeck: MvUseSpeck? get() = this.rootPath().parent as? MvUseSpeck

val MvPath.isUseSpeck get() = useSpeck != null