package org.move.toml.errorAnnotator

import org.intellij.lang.annotations.Language
import org.move.toml.MoveTomlErrorAnnotator
import org.move.utils.tests.annotation.AnnotatorTestCase

abstract class MoveTomlAnnotatorTestBase: AnnotatorTestCase(MoveTomlErrorAnnotator::class) {
    protected fun checkMoveTomlWarnings(@Language("TOML") text: String) =
        annotationFixture.check(text,
                                configure = { tomlText ->
                                    annotationFixture.codeInsightFixture
                                        .configureByText("Move.toml", tomlText)
                                })
}