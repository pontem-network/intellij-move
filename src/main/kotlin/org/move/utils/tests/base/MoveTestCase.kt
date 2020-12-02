package org.move.utils.tests.base

interface MoveTestCase: TestCase {
    override val testFileExtension: String get() = "move"
}
