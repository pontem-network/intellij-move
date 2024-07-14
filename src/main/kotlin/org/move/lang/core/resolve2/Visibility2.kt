package org.move.lang.core.resolve2

import org.move.lang.core.psi.MvElement
import org.move.lang.core.psi.MvModule
import org.move.lang.core.resolve.ModInfo
import org.move.lang.core.resolve.VisibilityFilter
import org.move.lang.core.resolve.VisibilityStatus
import org.move.lang.core.resolve.ref.Visibility
import org.move.lang.core.resolve.ref.Visibility.Public

fun getModInfo(module: MvModule): ModInfo {
    return ModInfo(module = module)
}

/** Creates filter which determines whether item with [this] visibility is visible from specific [ModInfo] */
fun Visibility.createFilter(): VisibilityFilter =
    if (this !is Public) {
        fun(context: MvElement, lazyModInfo: Lazy<ModInfo?>?): VisibilityStatus {
            val modInfo = lazyModInfo?.value ?: return VisibilityStatus.Invisible
            return VisibilityStatus.Visible
        }
    } else {
        { _, _ -> VisibilityStatus.Visible }
    }
