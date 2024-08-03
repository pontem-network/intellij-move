package org.move.lang.core.resolve2

import org.move.cli.containingMovePackage
import org.move.cli.settings.moveSettings
import org.move.ide.inspections.imports.usageScope
import org.move.lang.core.psi.*
import org.move.lang.core.psi.NamedItemScope.MAIN
import org.move.lang.core.psi.ext.*
import org.move.lang.core.resolve.ModInfo
import org.move.lang.core.resolve.VisibilityFilter
import org.move.lang.core.resolve.VisibilityStatus.Invisible
import org.move.lang.core.resolve.VisibilityStatus.Visible
import org.move.lang.core.resolve.ref.Namespace.NAME
import org.move.lang.core.resolve.ref.Namespace.TYPE
import org.move.lang.core.resolve.ref.Visibility2
import org.move.lang.core.resolve.ref.Visibility2.*

data class ItemVisibilityInfo(
    val item: MvNamedElement,
    val usageScope: NamedItemScope,
    val vis: Visibility2,
)

fun MvNamedElement.visInfo(adjustScope: NamedItemScope = MAIN): ItemVisibilityInfo {
    // todo: can be lazy
    val itemUsageScope = this.itemScope.shrinkScope(adjustScope)
    val visibility = (this as? MvVisibilityOwner)?.visibility2 ?: Public
    return ItemVisibilityInfo(this, usageScope = itemUsageScope, vis = visibility)
}

/** Creates filter which determines whether item with [this] visibility is visible from specific [ModInfo] */
fun ItemVisibilityInfo.createFilter(): VisibilityFilter {
    val (item, itemUsageScope, visibility) = this
    return VisibilityFilter { element, namespaces ->

        // inside msl everything is visible
        if (element.isMsl()) return@VisibilityFilter Visible

        // if inside MvAttrItem like abort_code=
        val attrItem = element.ancestorStrict<MvAttrItem>()
        if (attrItem != null) return@VisibilityFilter Visible

        val pathUsageScope = element.usageScope

        val path = element as? MvPath
        if (path != null) {
            val useSpeck = path.useSpeck
            if (useSpeck != null) {
                // inside import, all visibilities except for private work
                if (visibility !is Private) return@VisibilityFilter Visible

                // msl-only items are available from imports
                if (item.isMslOnlyItem) return@VisibilityFilter Visible

                // consts are importable in tests
                if (pathUsageScope.isTest && namespaces.contains(NAME)) return@VisibilityFilter Visible
            }
        }

        // #[test] functions cannot be used from non-imports
        if (item is MvFunction && item.hasTestAttr) return@VisibilityFilter Invisible

        val itemModule = item.containingModule
        // 0x0::builtins module items are always visible
        if (itemModule != null && itemModule.isBuiltins) return@VisibilityFilter Visible

        // #[test_only] items in non-test-only scope
        if (itemUsageScope != MAIN) {
            // cannot be used everywhere, need to check for scope compatibility
            if (itemUsageScope != pathUsageScope) return@VisibilityFilter Invisible
        }

        // we're in non-msl scope at this point, msl only items aren't available
        if (item is MslOnlyElement) return@VisibilityFilter Invisible

        val pathModule = element.containingModule
        // local methods, Self::method - everything is visible
        if (itemModule == pathModule) return@VisibilityFilter Visible

        // types visibility is ignored, their correct usage is checked in a separate inspection
        if (namespaces.contains(TYPE)) return@VisibilityFilter Visible

        when (visibility) {
            is Restricted -> {
                when (visibility) {
                    is Restricted.Friend -> {
                        if (pathModule != null) {
                            val friendModules = visibility.friendModules.value
                            if (friendModules.any { isModulesEqual(it, pathModule) }) return@VisibilityFilter Visible
                        }
                        Invisible
                    }
                    is Restricted.Script -> {
                        val containingFunction = element.containingFunction
                        if (containingFunction != null) {
                            if (containingFunction.isEntry || containingFunction.isPublicScript
                            ) return@VisibilityFilter Visible
                        }
                        if (element.containingScript != null) return@VisibilityFilter Visible
                        Invisible
                    }
                    is Restricted.Package -> {
                        if (!item.project.moveSettings.enablePublicPackage) {
                            return@VisibilityFilter Invisible
                        }
                        val pathPackage = element.containingMovePackage ?: return@VisibilityFilter Invisible
                        if (visibility.originPackage == pathPackage) Visible else Invisible
                    }
                }
            }
            is Public -> Visible
            is Private -> Invisible
        }
    }
}