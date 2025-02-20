package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import org.move.cli.settings.debugErrorOrFallback
import org.move.ide.annotator.BUILTIN_TYPE_IDENTIFIERS
import org.move.ide.annotator.PRIMITIVE_TYPE_IDENTIFIERS
import org.move.ide.annotator.SPEC_ONLY_PRIMITIVE_TYPES
import org.move.lang.MvElementTypes.COLON_COLON
import org.move.lang.core.psi.*
import org.move.lang.core.resolve.ref.ALL_NAMESPACES
import org.move.lang.core.resolve.ref.ENUMS
import org.move.lang.core.resolve.ref.ENUMS_N_MODULES
import org.move.lang.core.resolve.ref.ITEM_NAMESPACES
import org.move.lang.core.resolve.ref.MODULES
import org.move.lang.core.resolve.ref.MvPathReference
import org.move.lang.core.resolve.ref.MvPathReferenceImpl
import org.move.lang.core.resolve.ref.NAMES
import org.move.lang.core.resolve.ref.NONE
import org.move.lang.core.resolve.ref.Namespace
import org.move.lang.core.resolve.ref.SCHEMAS
import org.move.lang.core.resolve.ref.TYPES_N_ENUMS
import org.move.lang.core.resolve.ref.TYPES_N_ENUMS_N_MODULES
import org.move.lang.core.resolve.ref.TYPES_N_ENUMS_N_NAMES

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

val MvPath.length: Int
    get() {
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
                val expr = this.ancestorStrict<MvPathExpr>() ?: return@let -1
                it.argumentExprs.indexOf(expr)
            }
        return ind == 1
    }

val MvPath.identifierName: String? get() = identifier?.text

val MvPath.maybeStruct get() = reference?.resolveFollowingAliases() as? MvStruct

val MvPath.maybeFieldsOwner get() = reference?.resolveFollowingAliases() as? MvFieldsOwner

val MvPath.maybeSchema get() = reference?.resolveFollowingAliases() as? MvSchema

fun MvPath.allowedNamespaces(isCompletion: Boolean = false): Set<Namespace> {
    val qualifier = this.path
    val parent = this.parent
    return when {
        // mod::foo::bar
        //      ^
        parent is MvPath && qualifier != null -> ENUMS
        // foo::bar
        //  ^
        parent is MvPath -> ENUMS_N_MODULES
        // use 0x1::foo::bar; | use 0x1::foo::{bar, baz}
        //               ^                     ^
        parent is MvUseSpeck -> ITEM_NAMESPACES
        // a: bar
        //     ^
        parent is MvPathType && qualifier == null ->
            if (isCompletion) TYPES_N_ENUMS_N_MODULES else TYPES_N_ENUMS
        // a: foo::bar
        //         ^
        parent is MvPathType && qualifier != null -> TYPES_N_ENUMS
        parent is MvCallExpr -> NAMES
        parent is MvPathExpr
                && this.hasAncestor<MvAttrItemInitializer>() -> ALL_NAMESPACES
        // TYPES for resource indexing, NAMES for vector indexing
        parent is MvPathExpr
                && parent.parent is MvIndexExpr -> TYPES_N_ENUMS_N_NAMES

        // can be anything in completion
        parent is MvPathExpr -> if (isCompletion) ALL_NAMESPACES else NAMES

        parent is MvSchemaLit
                || parent is MvSchemaRef -> SCHEMAS
        parent is MvStructLitExpr
                || parent is MvPatStruct
                || parent is MvPatConst
                || parent is MvPatTupleStruct -> TYPES_N_ENUMS
        parent is MvAccessSpecifier -> TYPES_N_ENUMS
        parent is MvAddressSpecifierArg -> NAMES
        parent is MvAddressSpecifierCallParam -> NAMES
        parent is MvFriendDecl -> MODULES
        parent is MvModuleSpec -> MODULES

        // should not be used for attr items
        parent is MvAttrItem -> NONE

        else -> debugErrorOrFallback(
            "Cannot build path namespaces: unhandled parent type ${parent?.elementType}",
            NAMES
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

    override fun getReference(): MvPathReference? {
        if (referenceName == null) return null
        return when (val parent = parent) {
            is MvAttrItem -> {
                val attrItemList = (parent.parent as? MvAttrItemList) ?: return null
                val parentAttrItem = (attrItemList.parent as? MvAttrItem)?.takeIf { it.isTest } ?: return null
                val attr = parentAttrItem.attr ?: return null
                val ownerFunction = attr.attributeOwner as? MvFunction ?: return null
                AttrItemReferenceImpl(this, ownerFunction)
            }
            else -> MvPathReferenceImpl(this)
        }
    }
}

val MvPath.hasColonColon: Boolean get() = colonColon != null

val MvPath.isColonColonNext: Boolean get() = nextNonWsSibling?.elementType == COLON_COLON

val MvPath.useSpeck: MvUseSpeck? get() = this.rootPath().parent as? MvUseSpeck

val MvPath.isUseSpeck get() = useSpeck != null