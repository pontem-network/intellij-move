package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import org.move.cli.settings.debugErrorOrFallback
import org.move.ide.annotator.BUILTIN_TYPE_IDENTIFIERS
import org.move.ide.annotator.PRIMITIVE_TYPE_IDENTIFIERS
import org.move.ide.annotator.SPEC_ONLY_PRIMITIVE_TYPES
import org.move.lang.MvElementTypes.COLON_COLON
import org.move.lang.core.psi.*
import org.move.lang.core.resolve.ref.*

/** For `Foo::bar::baz::quux` path returns `Foo` */
tailrec fun MvPath.basePath(): MvPath {
    val qualifier = this.path
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
            ?.let {
                val path = it.path ?: return@let null
                if (path.textMatches("update_field")) it else null
            }
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

fun MvPath.allowedNamespaces(isCompletion: Boolean = false): NsSet {
    val qualifier = this.path
    val parent = this.parent
    return when {
        // mod::foo::bar
        //      ^
        // note: it technically can be module with address, but for those cases this function is not called
        parent is MvPath && qualifier != null -> ENUMS
        // foo::bar
        //  ^
        parent is MvPath -> {
            ENUMS_N_MODULES
//            // if we're inside PathType, then ENUM::ENUM_VARIANT cannot be used, so foo cannot be enum
//            if (parent.parent is MvPathType) {
//                MODULES
//            } else {
//                TYPES_N_MODULES
//            }
        }
        // use 0x1::foo::bar; | use 0x1::foo::{bar, baz}
        //               ^                     ^
        parent is MvUseSpeck -> IMPORTABLE_NS

        parent is MvPathType
                && parent.parent is MvIsExpr -> TYPES_N_ENUMS_N_ENUM_VARIANTS

        // a: bar
        //     ^
        parent is MvPathType && qualifier == null -> if (isCompletion) TYPES_N_ENUMS_N_MODULES else TYPES_N_ENUMS
        // a: foo::bar
        //         ^
        parent is MvPathType && qualifier != null -> TYPES_N_ENUMS
//        parent is MvCallExpr -> NAMES_N_ENUM_VARIANTS
        parent is MvCallExpr -> NAMES_N_FUNCTIONS_N_ENUM_VARIANTS
        // all ns allowed in attributes
        parent is MvPathExpr
                && this.hasAncestor<MvAttrItemInitializer>() -> ALL_NS
        // TYPES for resource indexing, NAMES for vector indexing
        parent is MvPathExpr
                && parent.parent is MvIndexExpr -> TYPES_N_ENUMS_N_NAMES

        // can be anything in completion
        parent is MvPathExpr -> if (isCompletion) ALL_NS else NAMES_N_FUNCTIONS_N_ENUM_VARIANTS

        parent is MvSchemaLit
                || parent is MvSchemaRef -> SCHEMAS
        parent is MvStructLitExpr
                || parent is MvPatStruct
                || parent is MvPatConst
                || parent is MvPatTupleStruct -> TYPES_N_ENUMS_N_ENUM_VARIANTS

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