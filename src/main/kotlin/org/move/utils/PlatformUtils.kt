package org.move.utils

import com.intellij.openapi.util.Computable
import com.intellij.openapi.util.RecursionManager

fun <T> recursionGuard(key: Any, block: Computable<T>, memoize: Boolean = false): T? =
    RecursionManager.doPreventingRecursion(key, memoize, block)
