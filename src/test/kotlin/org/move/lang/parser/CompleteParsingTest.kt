package org.move.lang.parser

import org.move.utils.tests.parser.MoveParsingTestCase

class CompleteParsingTest : MoveParsingTestCase("complete") {
//    fun `test while loop inline assignment`() = doTest(true)
//    fun `test contextual token operators`() = doTest(true)
//    fun `test generics`() = doTest(true)
//    fun `test annotated literals`() = doTest(true)
//    fun `test structs`() = doTest(true)
//    fun `test assignment lhs`() = doTest(true)
//    fun `test comments`() = doTest(true)

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

    // let patterns
    fun `test let patterns`() = doTest(true)
}