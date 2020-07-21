package org.move.lang.utils.tests.annotator

import com.intellij.testFramework.fixtures.BasePlatformTestCase

abstract class MvAnnotationTestBase: BasePlatformTestCase() {
    protected lateinit var annotationFixture: MvAnnotationTestFixture
}