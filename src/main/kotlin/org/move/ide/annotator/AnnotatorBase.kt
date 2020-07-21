/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.move.ide.annotator

import com.intellij.lang.annotation.Annotator
import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import com.intellij.util.containers.ContainerUtil
import org.jetbrains.annotations.TestOnly

abstract class AnnotatorBase : Annotator {
    companion object {
        private val enabledAnnotators: MutableSet<Class<out AnnotatorBase>> = ContainerUtil.newConcurrentSet()

        @TestOnly
        fun enableAnnotator(annotatorClass: Class<out AnnotatorBase>, parentDisposable: Disposable) {
            enabledAnnotators += annotatorClass
            Disposer.register(parentDisposable, Disposable { enabledAnnotators -= annotatorClass })
        }
    }
}
