/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.move.utils.tests.annotation

import org.move.ide.annotator.MvAnnotator
import kotlin.reflect.KClass

abstract class AnnotatorTestCase(
    private val annotatorClass: KClass<out MvAnnotator>
) : MvAnnotationTestCase() {

    override fun createAnnotationFixture(): MvAnnotationTestFixture = MvAnnotationTestFixture(
        this, myFixture, annotatorClasses = listOf(annotatorClass)
    )
}
