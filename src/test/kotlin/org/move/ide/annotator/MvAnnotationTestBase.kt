package org.move.ide.annotator

import com.intellij.testFramework.fixtures.BasePlatformTestCase

abstract class MvAnnotationTestBase: BasePlatformTestCase() {
    protected lateinit var annotationFixture: MvAnnotationTestFixture
}