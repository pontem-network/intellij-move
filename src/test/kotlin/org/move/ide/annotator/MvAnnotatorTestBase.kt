/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.move.ide.annotator

import kotlin.reflect.KClass

abstract class MvAnnotatorTestBase(private val annotatorClass: KClass<out AnnotatorBase>) :
    MvAnnotationTestBase() {

    fun createAnnotationFixture(): MvAnnotationTestFixture =
        MvAnnotationTestFixture(myFixture, annotatorClasses = listOf(annotatorClass))

    fun checkError(text: String) = createAnnotationFixture().check(text)
}
