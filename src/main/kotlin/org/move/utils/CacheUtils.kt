package org.move.utils

import com.intellij.injected.editor.VirtualFileWindow
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.UserDataHolder
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import org.move.lang.core.psi.MvCodeFragment
import org.move.lang.core.psi.MvElement
import org.move.lang.core.psi.MvModificationTrackerOwner
import org.move.lang.core.psi.ext.contextOrSelf
import org.move.lang.core.psi.moveStructureModificationTracker

val Project.cacheManager: CachedValuesManager get() = CachedValuesManager.getManager(this)

fun <T> CachedValuesManager.cache(
    dataHolder: UserDataHolder,
    key: Key<CachedValue<T>>,
    provider: CachedValueProvider<T>
): T {
    return getCachedValue(dataHolder, key, provider, false)
}

fun <T> MvElement.createCachedResult(value: T): CachedValueProvider.Result<T> {
    val structureModificationTracker = project.moveStructureModificationTracker
    return when {
        // The case of injected language. Injected PSI don't have its own event system, so can only
        // handle evens from outer PSI. For example, Rust language is injected to Kotlin's string
        // literal. If a user change the literal, we can only be notified that the literal is changed.
        // So we have to invalidate the cached value on any PSI change
        containingFile.virtualFile is VirtualFileWindow -> {
            CachedValueProvider.Result.create(
                value,
                com.intellij.psi.util.PsiModificationTracker.MODIFICATION_COUNT
            )
        }

        // Invalidate cached value of code fragment on any PSI change
        this is MvCodeFragment -> CachedValueProvider.Result.create(
            value,
            com.intellij.psi.util.PsiModificationTracker.MODIFICATION_COUNT
        )

        // CachedValueProvider.Result can accept a ModificationTracker as a dependency, so the
        // cached value will be invalidated if the modification counter is incremented.
        else -> {
            val modificationTracker = contextOrSelf<MvModificationTrackerOwner>()?.modificationTracker
            CachedValueProvider.Result.create(
                value,
                listOfNotNull(structureModificationTracker, modificationTracker)
            )
        }
    }
}
