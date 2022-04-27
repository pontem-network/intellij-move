package org.move.utils.tests.annotation

import com.intellij.codeInspection.InspectionProfileEntry
import org.intellij.lang.annotations.Language
import kotlin.reflect.KClass

abstract class InspectionTestBase(
    private val inspectionClass: KClass<out InspectionProfileEntry>
) : MvAnnotationTestCase() {

    protected lateinit var inspection: InspectionProfileEntry

    override fun setUp() {
        super.setUp()
        inspection = annotationFixture.enabledInspections[0]
    }

    override fun createAnnotationFixture(): MvAnnotationTestFixture =
        MvAnnotationTestFixture(this, myFixture, inspectionClasses = listOf(inspectionClass))

    fun checkFixIsUnavailable(
        fixName: String,
        @Language("Move") text: String,
        checkWarn: Boolean = true,
        checkInfo: Boolean = false,
        checkWeakWarn: Boolean = false,
        ignoreExtraHighlighting: Boolean = false,
    ) =
        annotationFixture.checkFixIsUnavailable(
            fixName, text, checkWarn, checkInfo, checkWeakWarn, ignoreExtraHighlighting)

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
