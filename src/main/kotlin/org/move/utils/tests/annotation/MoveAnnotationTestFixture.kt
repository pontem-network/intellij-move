/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.move.utils.tests.annotation

import com.intellij.codeInsight.daemon.impl.SeveritiesProvider
import com.intellij.codeInspection.InspectionProfileEntry
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.project.Project
import com.intellij.testFramework.InspectionTestUtil
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import com.intellij.testFramework.fixtures.impl.BaseFixture
import junit.framework.TestCase
import org.intellij.lang.annotations.Language
import org.move.ide.annotator.MoveAnnotator
import kotlin.reflect.KClass

class MoveAnnotationTestFixture(
    private val testCase: TestCase,
    private val codeInsightFixture: CodeInsightTestFixture,
    private val annotatorClasses: List<KClass<out MoveAnnotator>> = emptyList(),
    private val inspectionClasses: List<KClass<out InspectionProfileEntry>> = emptyList(),
) : BaseFixture() {
    val project: Project get() = codeInsightFixture.project
    lateinit var enabledInspections: List<InspectionProfileEntry>

    override fun setUp() {
        super.setUp()
        annotatorClasses.forEach {
            MoveAnnotator.enableAnnotator(
                it.java,
                testRootDisposable
            )
        }
        enabledInspections = InspectionTestUtil.instantiateTools(inspectionClasses.map { it.java })
        codeInsightFixture.enableInspections(*enabledInspections.toTypedArray())
    }

    private fun replaceCaretMarker(text: String) = text.replace("/*caret*/", "<caret>")

    fun checkHighlighting(text: String, ignoreExtraHighlighting: Boolean) = checkByText(
        text,
        checkWarn = false,
        checkWeakWarn = false,
        checkInfo = false,
        ignoreExtraHighlighting = ignoreExtraHighlighting
    )

    fun checkInfo(text: String) =
        checkByText(text, checkWarn = false, checkWeakWarn = false, checkInfo = true)

    fun checkWarnings(text: String) =
        checkByText(text, checkWarn = true, checkWeakWarn = true, checkInfo = false)

    fun checkErrors(text: String) =
        checkByText(text, checkWarn = false, checkWeakWarn = false, checkInfo = false)

    private fun configureByText(text: String) {
        codeInsightFixture.configureByText("main.move", replaceCaretMarker(text.trimIndent()))
    }

    fun checkByText(
        text: String,
        checkWarn: Boolean = true,
        checkInfo: Boolean = false,
        checkWeakWarn: Boolean = false,
        ignoreExtraHighlighting: Boolean = false,
    ) = check(
        text,
        checkWarn = checkWarn,
        checkInfo = checkInfo,
        checkWeakWarn = checkWeakWarn,
        ignoreExtraHighlighting = ignoreExtraHighlighting,
        configure = this::configureByText
    )

    fun check(
        text: String,
        checkWarn: Boolean = true,
        checkInfo: Boolean = false,
        checkWeakWarn: Boolean = false,
        ignoreExtraHighlighting: Boolean = false,
        configure: (String) -> Unit
    ) {
        configure(text)
        codeInsightFixture.checkHighlighting(checkWarn, checkInfo, checkWeakWarn, ignoreExtraHighlighting)
    }

    fun registerSeverities(severities: List<HighlightSeverity>) {
        val testSeverityProvider = TestSeverityProvider(severities)
        SeveritiesProvider.EP_NAME.point.registerExtension(testSeverityProvider, testRootDisposable)
    }

    fun checkFixIsUnavailable(
        fixName: String,
        @Language("Move") text: String,
        checkWarn: Boolean,
        checkInfo: Boolean,
        checkWeakWarn: Boolean,
        ignoreExtraHighlighting: Boolean,
//        configure: (String) -> Unit,
    ) {
        check(text, checkWarn, checkInfo, checkWeakWarn, ignoreExtraHighlighting, this::configureByText)
        check(
            codeInsightFixture.filterAvailableIntentions(fixName).isEmpty()
        ) {
            "Fix $fixName should not be possible to apply."
        }
    }

    fun applyQuickFix(name: String) {
        val action = codeInsightFixture.findSingleIntention(name)
        codeInsightFixture.launchAction(action)
    }

    protected fun checkFix(
        fixName: String,
        before: String,
        after: String,
        configure: (String) -> Unit,
        checkBefore: () -> Unit,
        checkAfter: (String) -> Unit,
    ) {
        configure(before)
        checkBefore()
        applyQuickFix(fixName)
        checkAfter(after)
    }

    fun checkFixByText(
        fixName: String,
        before: String,
        after: String,
        checkWarn: Boolean = true,
        checkInfo: Boolean = false,
        checkWeakWarn: Boolean = false,
    ) = checkFix(
        fixName, before, after,
        configure = this::configureByText,
        checkBefore = { codeInsightFixture.checkHighlighting(checkWarn, checkInfo, checkWeakWarn) },
        checkAfter = this::checkByText
    )
}
