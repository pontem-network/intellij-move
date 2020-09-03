package org.move.utils.tests.annotation

import org.intellij.lang.annotations.Language
import org.move.utils.tests.MoveTestCase

abstract class MoveAnnotationTestCase : MoveTestCase() {
    protected lateinit var annotationFixture: MoveAnnotationTestFixture

    override fun setUp() {
        super.setUp()
        annotationFixture = createAnnotationFixture()
        annotationFixture.setUp()
    }

    override fun tearDown() {
        annotationFixture.tearDown()
        super.tearDown()
    }

    protected abstract fun createAnnotationFixture(): MoveAnnotationTestFixture

    fun checkHighlighting(text: String, ignoreExtraHighlighting: Boolean = true) =
        annotationFixture.checkHighlighting(
            text,
            ignoreExtraHighlighting
        )

    fun checkErrors(text: String) = annotationFixture.checkErrors(text)
}