package org.move.lang.core.resolve

import com.intellij.psi.PsiFile
import com.intellij.psi.util.CachedValueProvider
import org.move.lang.core.psi.MvElement
import org.move.utils.cache
import org.move.utils.cacheManager

interface PsiCachedValueProvider<T>: CachedValueProvider<T> {
    val owner: MvElement
    abstract override fun compute(): CachedValueProvider.Result<T>
}

fun <T> PsiCachedValueProvider<T>.getResults(): T {
    val manager = this.owner.project.cacheManager
    val key = manager.getKeyForClass<T>(this::class.java)
    return manager.cache(owner, key, this)
}


interface PsiFileCachedValueProvider<T>: CachedValueProvider<T> {
    val file: PsiFile
    abstract override fun compute(): CachedValueProvider.Result<T>
}

fun <T> PsiFileCachedValueProvider<T>.getResults(): T {
    val manager = this.file.project.cacheManager
    val key = manager.getKeyForClass<T>(this::class.java)
    return manager.cache(file, key, this)
}
