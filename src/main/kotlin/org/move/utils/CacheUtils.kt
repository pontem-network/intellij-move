package org.move.utils

import com.intellij.injected.editor.VirtualFileWindow
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ModificationTracker
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import org.move.lang.MoveFile
import org.move.lang.core.psi.MvElement
import org.move.lang.core.psi.moveStructureModificationTracker

interface PsiCachedValueProvider<T>: CachedValueProvider<T> {
    val owner: MvElement
    abstract override fun compute(): CachedValueProvider.Result<T>
}

fun <T> PsiCachedValueProvider<T>.getResults(): T {
    val manager = this.owner.project.cacheManager
    val key = manager.getKeyForClass<T>(this::class.java)
    return manager.getCachedValue(this.owner, key, this, false)
}


interface MoveFileCachedValueProvider<T>: CachedValueProvider<T> {
    val file: MoveFile
    abstract override fun compute(): CachedValueProvider.Result<T>
}

fun <T> MoveFileCachedValueProvider<T>.getResults(): T {
    val manager = this.file.project.cacheManager
    val key = manager.getKeyForClass<T>(this::class.java)
    return manager.getCachedValue(this.file, key, this, false)
}

val Project.cacheManager: CachedValuesManager get() = CachedValuesManager.getManager(this)

fun <T> MvElement.psiCacheResult(value: T): CachedValueProvider.Result<T> =
    CachedValueProvider.Result.create(
        value,
        PsiModificationTracker.MODIFICATION_COUNT
    )

fun <T> Project.moveStructureCacheResult(value: T): CachedValueProvider.Result<T> {
    return CachedValueProvider.Result.create(
        value,
        this.moveStructureModificationTracker
    )
}

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
