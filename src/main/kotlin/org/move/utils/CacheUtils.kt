package org.move.utils

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.UserDataHolder
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager

val Project.cacheManager: CachedValuesManager get() = CachedValuesManager.getManager(this)

fun <T> CachedValuesManager.cache(
    dataHolder: UserDataHolder,
    key: Key<CachedValue<T>>,
    provider: CachedValueProvider<T>
): T {
    return getCachedValue(dataHolder, key, provider, false)
}
