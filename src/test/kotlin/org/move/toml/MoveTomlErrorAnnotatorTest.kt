package org.move.toml

import org.move.utils.tests.annotation.AnnotatorTestCase

class MoveTomlErrorAnnotatorTest : AnnotatorTestCase(MoveTomlErrorAnnotator::class) {
    fun `test valid addresses`() = checkMoveTomlWarnings("""
        [addresses]
        addr1 = "0x1"
        addr2 = "0x42"
        addr3 = "0x4242424242424242424242424242424242424242424242424242420000000000"
    """)

    fun `test invalid addresses`() = checkMoveTomlWarnings("""
        [addresses]
        # invalid symbols
        addr2 = "0xhelloworld"
        # too long
        addr3 = "0x424242424242424242424242424242424242424242424242424242000000000011122"
    """)
}