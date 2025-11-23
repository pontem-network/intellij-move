package org.move.utils.tests

import com.intellij.testFramework.PlatformTestUtil
import com.intellij.util.ConcurrencyUtil
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@Throws(InterruptedException::class)
fun CountDownLatch.waitFinished(timeoutMs: Long): Boolean {
    for (i in 1..timeoutMs / ConcurrencyUtil.DEFAULT_TIMEOUT_MS) {
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue()
        if (await(ConcurrencyUtil.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS)) return true
    }
    return false
}
