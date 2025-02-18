package org.move.lang.core.resolve

import org.move.cli.containingMovePackage
import org.move.cli.settings.moveSettings
import org.move.ide.inspections.imports.usageScope
import org.move.lang.core.psi.*
import org.move.lang.core.psi.NamedItemScope.MAIN
import org.move.lang.core.psi.ext.*
import org.move.lang.core.resolve.VisibilityStatus.Invisible
import org.move.lang.core.resolve.VisibilityStatus.Visible
import org.move.lang.core.resolve.ref.Namespace.*
import org.move.lang.core.resolve.ref.Visibility2
import org.move.lang.core.resolve.ref.Visibility2.*
import org.move.stdext.containsAny

data class ItemVisibilityInfo(
    val item: MvNamedElement,
    val itemScopeAdjustment: NamedItemScope,
    val vis: Visibility2,
)

fun MvNamedElement.visInfo(adjustScope: NamedItemScope = MAIN): ItemVisibilityInfo {
    val visibility = (this as? MvVisibilityOwner)?.visibility2 ?: Public
    return ItemVisibilityInfo(this, adjustScope, visibility)
}

/** Creates filter which determines whether item with [this] visibility is visible from specific [ModInfo] */
fun ItemVisibilityInfo.createFilter(): VisibilityFilter {
    val (item, itemScopeAdjustment, visibility) = this
    return VisibilityFilter { context, itemNs ->

        // inside msl everything is visible
        if (context.isMsl()) return@VisibilityFilter Visible

        // if inside MvAttrItem like abort_code=
        val attrItem = context.ancestorStrict<MvAttrItem>()
        if (attrItem != null) return@VisibilityFilter Visible

        val pathUsageScope = context.usageScope

        val path = context as? MvPath
        if (path != null) {
            val useSpeck = path.useSpeck
            if (useSpeck != null) {
                // for use specks, items needs to be public to be visible, no other rules apply
                if (item is MvItemElement && item.isPublic) return@VisibilityFilter Visible

                // if item does not support visibility, then it is always private

                // msl-only items are available from imports
                if (item.isMslOnlyItem) return@VisibilityFilter Visible

                // consts are importable in tests
                if (pathUsageScope.isTest && itemNs.contains(NAME)) return@VisibilityFilter Visible
            }
        }

        // #[test] functions cannot be used from non-imports
        if (item is MvFunction && item.hasTestAttr) return@VisibilityFilter Invisible

        val itemModule = item.containingModule
        // 0x0::builtins module items are always visible
        if (itemModule != null && itemModule.isBuiltins) return@VisibilityFilter Visible

        val itemUsageScope = item.usageScope.shrinkScope(itemScopeAdjustment)
        // #[test_only] items in non-test-only scope
        if (itemUsageScope != MAIN) {
            // cannot be used everywhere, need to check for scope compatibility
            if (itemUsageScope != pathUsageScope) return@VisibilityFilter Invisible
        }

        // we're in non-msl scope at this point, msl only items aren't available
        if (item is MslOnlyElement) return@VisibilityFilter Invisible

        val pathModule = context.containingModule
        // local methods, Self::method - everything is visible
        if (itemModule == pathModule) return@VisibilityFilter Visible

        // item is type, check whether it's allowed in the context
        if (itemNs.containsAny(TYPE, ENUM)) {
            val rootPath = path?.rootPath()
            when (rootPath?.parent) {
                // todo: when structs and enums can be public, conditions for struct lit/pat should be added here
                is MvPathType -> return@VisibilityFilter Visible
            }
        }

        when (visibility) {
            is Restricted -> {
                when (visibility) {
                    is Restricted.Friend -> {
                        if (pathModule != null && itemModule != null) {
                            val friendModules = itemModule.friendModules
                            if (friendModules.any { isModulesEqual(it, pathModule) }) {
                                return@VisibilityFilter Visible
                            }
                        }
                        Invisible
                    }
                    is Restricted.Package -> {
                        if (!item.project.moveSettings.enablePublicPackage) {
                            return@VisibilityFilter Invisible
                        }
                        val pathPackage =
                            context.containingMovePackage ?: return@VisibilityFilter Invisible
                        val itemPackage = item.containingMovePackage ?: return@VisibilityFilter Invisible
//                        val originPackage = visibility.originPackage.value ?: return@VisibilityFilter Invisible
                        if (pathPackage == itemPackage) Visible else Invisible
                    }
                }
            }
            is Public -> Visible
            is Private -> Invisible
        }
    }
}