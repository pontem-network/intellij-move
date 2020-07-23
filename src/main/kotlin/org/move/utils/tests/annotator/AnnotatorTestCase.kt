/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.move.utils.tests.annotator

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.move.ide.annotator.AnnotatorBase
import kotlin.reflect.KClass

abstract class AnnotatorTestCase(private val annotatorClass: KClass<out AnnotatorBase>) :
    BasePlatformTestCase() {

    fun checkError(text: String) = createAnnotatorFixture().check(text)

    private fun createAnnotatorFixture(): AnnotatorTestFixture =
        AnnotatorTestFixture(
            myFixture,
            annotatorClasses = listOf(annotatorClass)
        )
}
