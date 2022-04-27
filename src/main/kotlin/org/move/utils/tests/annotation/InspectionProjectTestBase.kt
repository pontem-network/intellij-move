package org.move.utils.tests.annotation

import com.intellij.codeInspection.InspectionProfileEntry
import org.move.utils.tests.FileTreeBuilder
import org.move.utils.tests.MvProjectTestBase
import kotlin.reflect.KClass

abstract class InspectionProjectTestBase(
    private val inspectionClass: KClass<out InspectionProfileEntry>
) : MvProjectTestBase() {

    protected lateinit var annotationFixture: MvAnnotationTestFixture
    protected lateinit var inspection: InspectionProfileEntry

    override fun setUp() {
        super.setUp()
        annotationFixture = MvAnnotationTestFixture(
            this,
            myFixture,
            inspectionClasses = listOf(inspectionClass)
        )
        annotationFixture.setUp()
        inspection = annotationFixture.enabledInspections[0]
    }

    override fun tearDown() {
        annotationFixture.tearDown()
        super.tearDown()
    }

//    protected fun createAnnotationFixture(): MvAnnotationTestFixture =
//        MvAnnotationTestFixture(this, myFixture, inspectionClasses = listOf(inspectionClass))

//    fun checkHighlighting(text: String) = annotationFixture.checkHighlighting(text)

//    fun checkErrors(@Language("Move") text: String) = annotationFixture.checkErrors(text)
//    fun checkWarnings(@Language("Move") text: String) = annotationFixture.checkWarnings(text)

    protected fun checkFixByFileTree(
        fixName: String,
        before: FileTreeBuilder.() -> Unit,
        after: String,
        checkWarn: Boolean = true,
        checkInfo: Boolean = false,
        checkWeakWarn: Boolean = false
    ) = annotationFixture.checkFixByFileTree(
        fixName,
        before,
        after,
        checkWarn,
        checkInfo,
        checkWeakWarn
    )

//    fun checkFixIsUnavailable(
//        fixName: String,
//        @Language("Move") text: String,
//        checkWarn: Boolean = true,
//        checkInfo: Boolean = false,
//        checkWeakWarn: Boolean = false,
//        ignoreExtraHighlighting: Boolean = false,
//    ) =
//        annotationFixture.checkFixIsUnavailable(
//            fixName, text, checkWarn, checkInfo, checkWeakWarn, ignoreExtraHighlighting
//        )

//    protected fun checkFixByText(
//        fixName: String,
//        @Language("Move") before: String,
//        @Language("Move") after: String,
//        checkWarn: Boolean = true,
//        checkInfo: Boolean = false,
//        checkWeakWarn: Boolean = false,
//    ) =
//        annotationFixture.checkFixByText(fixName, before, after, checkWarn, checkInfo, checkWeakWarn)
}
