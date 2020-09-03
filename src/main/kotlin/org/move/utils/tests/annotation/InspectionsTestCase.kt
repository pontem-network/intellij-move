package org.move.utils.tests.annotation

import com.intellij.codeInspection.InspectionProfileEntry
import kotlin.reflect.KClass

class InspectionsTestCase(
    private val inspectionClass: KClass<out InspectionProfileEntry>
) : MoveAnnotationTestCase() {

    override fun createAnnotationFixture(): MoveAnnotationTestFixture =
        MoveAnnotationTestFixture(this, myFixture, inspectionClasses = listOf(inspectionClass))

//    protected lateinit var inspection: InspectionProfileEntry

//    override fun setUp() {
//        super.setUp()
//        inspection = annotationFixture.enabledInspections[0]
//    }

//    private fun enableInspection() = myFixture.enableInspections(inspection)
//
//    override fun configureByText(text: String) {
//        super.configureByText(text)
//        enableInspection()
//    }
}