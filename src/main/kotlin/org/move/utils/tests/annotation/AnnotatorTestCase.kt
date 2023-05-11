/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.move.utils.tests.annotation

import org.intellij.lang.annotations.Language
import org.move.ide.annotator.MvAnnotatorBase
import kotlin.reflect.KClass

abstract class AnnotatorTestCase(
    private val annotatorClass: KClass<out MvAnnotatorBase>
) : MvAnnotationTestCase() {

    override fun createAnnotationFixture(): MvAnnotationTestFixture =
        MvAnnotationTestFixture(this, myFixture, annotatorClasses = listOf(annotatorClass))

    protected fun checkFixByText(
        fixName: String,
        @Language("Move") before: String,
        @Language("Move") after: String,
        checkWarn: Boolean = true,
        checkInfo: Boolean = false,
        checkWeakWarn: Boolean = false,
    ) =
        annotationFixture.checkFixByText(fixName, before, after, checkWarn, checkInfo, checkWeakWarn)
}
