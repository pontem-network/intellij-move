/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.move.utils.tests.annotator

import com.intellij.codeInsight.daemon.impl.SeveritiesProvider
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.move.ide.annotator.AnnotatorBase
import kotlin.reflect.KClass

abstract class AnnotatorTestCase(private val annotatorClass: KClass<out AnnotatorBase>) :
    BasePlatformTestCase() {

    fun checkError(text: String) = createAnnotatorFixture().check(text)
    fun checkHighlighting(text: String) =
        createAnnotatorFixture().check(text, checkWarn = false, checkInfo = false, checkWeakWarn = false)

    protected fun createAnnotatorFixture(): AnnotatorTestFixture =
        AnnotatorTestFixture(
            myFixture,
            annotatorClasses = listOf(annotatorClass)
        )
}
