package org.move.utils

import com.intellij.injected.editor.VirtualFileWindow
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.ModificationTracker
import com.intellij.openapi.util.UserDataHolder
import com.intellij.psi.util.*
import com.intellij.psi.util.CachedValueProvider.Result
import org.move.lang.core.psi.MvCodeFragment
import org.move.lang.core.psi.MvElement

val Project.cacheManager: CachedValuesManager get() = CachedValuesManager.getManager(this)

fun <T> Project.globalPsiDependentCache(
    key: Key<CachedValue<T>>,
    provider: (Project) -> T
): T {
    val cacheManager = this.cacheManager
    return cacheManager.getCachedValue(this, key, {
        Result.create(provider(this), PsiModificationTracker.MODIFICATION_COUNT)
    }, false)
}

fun <T> CachedValuesManager.cache(
    dataHolder: UserDataHolder,
    key: Key<CachedValue<T>>,
    provider: CachedValueProvider<T>
): T {
    return getCachedValue(dataHolder, key, provider, false)
}

fun <T> MvElement.psiCacheResult(value: T): CachedValueProvider.Result<T> =
    this.cacheResult(value, listOf(PsiModificationTracker.MODIFICATION_COUNT))

fun <T> MvElement.cacheResult(value: T, dependencies: List<Any>): CachedValueProvider.Result<T> {
    return when {
        // The case of injected language. Injected PSI don't have its own event system, so can only
        // handle evens from outer PSI. For example, Rust language is injected to Kotlin's string
        // literal. If a user change the literal, we can only be notified that the literal is changed.
        // So we have to invalidate the cached value on any PSI change
        containingFile.virtualFile is VirtualFileWindow -> {
            CachedValueProvider.Result.create(
                value,
                PsiModificationTracker.MODIFICATION_COUNT
            )
        }

        // Invalidate cached value of code fragment on any PSI change
        this is MvCodeFragment -> CachedValueProvider.Result.create(
            value,
            PsiModificationTracker.MODIFICATION_COUNT
        )

        else -> CachedValueProvider.Result.create(value, dependencies)
    }
}

val MvElement.containingFileModificationTracker: ModificationTracker?
    get() =
        this.containingFile?.originalFile?.virtualFile

fun <T> MvElement.psiFileTrackedCachedResult(value: T): CachedValueProvider.Result<T> {
    val fileModificationTracker = this.containingFileModificationTracker
    return if (fileModificationTracker != null) {
        CachedValueProvider.Result.create(value, fileModificationTracker)
    } else {
        CachedValueProvider.Result.create(value, PsiModificationTracker.MODIFICATION_COUNT)
    }
}
