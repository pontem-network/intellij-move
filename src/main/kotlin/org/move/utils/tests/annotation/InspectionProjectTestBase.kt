package org.move.utils.tests.annotation

import com.intellij.codeInspection.InspectionProfileEntry
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.util.indexing.FileBasedIndex
import org.move.ide.inspections.imports.MvNamedElementIndex
import org.move.utils.tests.FileTreeBuilder
import org.move.utils.tests.MvProjectTestBase
import org.move.utils.tests.replaceCaretMarker
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

    protected fun checkFixByFileTree(
        fixName: String,
        before: FileTreeBuilder.() -> Unit,
        after: String,
        checkWarn: Boolean = true,
        checkInfo: Boolean = false,
        checkWeakWarn: Boolean = false
    ) {
        testProject(before)

        FileBasedIndex.getInstance().requestRebuild(MvNamedElementIndex.KEY)
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue()

        annotationFixture.codeInsightFixture.checkHighlighting(checkWarn, checkInfo, checkWeakWarn)
        annotationFixture.applyQuickFix(fixName)
        annotationFixture.codeInsightFixture.checkResult(replaceCaretMarker(after.trimIndent()))
    }

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
