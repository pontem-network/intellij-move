package org.move.utils.tests.annotation

import org.intellij.lang.annotations.Language
import org.move.utils.tests.MoveTestBase

abstract class MoveAnnotationTestCase : MoveTestBase() {
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

    fun checkErrors(@Language("Move") text: String) = annotationFixture.checkErrors(text)

    protected fun checkByText(
        @Language("Move") text: String,
        checkWarn: Boolean = true,
        checkInfo: Boolean = false,
        checkWeakWarn: Boolean = false,
        ignoreExtraHighlighting: Boolean = false,
    ) = annotationFixture.checkByText(
        text,
        checkWarn,
        checkInfo,
        checkWeakWarn,
        ignoreExtraHighlighting,
    )

}
