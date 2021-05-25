package org.move.lang.parser

import org.move.utils.tests.parser.MoveParsingTestCase

class CompleteParsingTest : MoveParsingTestCase("complete") {
    fun `test comments`() = doTest(true)
    fun `test addresses`() = doTest(true)

    // functions
    fun `test function declarations`() = doTest(true)
    fun `test function calls`() = doTest(true)

    //expressions
    fun `test expressions`() = doTest(true)
    fun `test expressions assignments`() = doTest(true)
    fun `test expressions if else as`() = doTest(true)
    fun `test expressions angle brackets`() = doTest(true)
    fun `test expressions specs`() = doTest(true)

    // use
    fun `test use`() = doTest(true)
    fun `test friend`() = doTest(true)

    // assignments
    fun `test let patterns`() = doTest(true)
    fun `test assignments`() = doTest(true)

    // structs
    fun `test struct declarations`() = doTest(true)
    fun `test struct literals`() = doTest(true)

    // misc
    fun `test while loop inline assignment`() = doTest(true)
    fun `test contextual token operators`() = doTest(true)
    fun `test generics`() = doTest(true)
    fun `test annotated literals`() = doTest(true)

    fun `test specs`() = doTest(true)

}
