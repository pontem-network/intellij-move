package org.move.utils

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.UserDataHolder
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker

fun <T> cacheWithPsiTracker(project: Project, dataHolder: UserDataHolder, compute: () -> T): T {
    return CachedValuesManager.getManager(project).getCachedValue(dataHolder) {
        val res = compute()
        CachedValueProvider.Result.create(res, PsiModificationTracker.MODIFICATION_COUNT)
    }
}
