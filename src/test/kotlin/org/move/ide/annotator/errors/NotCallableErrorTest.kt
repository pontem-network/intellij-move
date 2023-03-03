package org.move.ide.annotator.errors

import org.move.ide.annotator.MvErrorAnnotator
import org.move.utils.tests.annotation.AnnotatorTestCase

class NotCallableErrorTest: AnnotatorTestCase(MvErrorAnnotator::class) {
    fun `test no error if function`() = checkErrors("""
module 0x1::m {
    fun call() {}
    fun main() {
        call();
    }
}        
    """)

    fun `test no error if spec function`() = checkErrors("""
module 0x1::m {
    spec fun call() {}
    fun main() {
        call();
    }
}        
    """)

    fun `test no error if lambda`() = checkErrors("""
module 0x1::m {
    inline fun call<Element>(f: |Element| Element) {
        f();
    }
}        
    """)

    fun `test error variable is not callable`() = checkErrors("""
module 0x1::m {
    fun main() {
        let num = 1;
        <error descr="'num' is not callable">num</error>();
    }
}        
    """)

    fun `test error struct is not callable`() = checkErrors("""
module 0x1::m {
    struct S {}
    fun main() {
        <error descr="'S' is not callable">S</error>();
    }
}        
    """)

    fun `test error function parameter is not callable`() = checkErrors("""
module 0x1::m {
    fun main(a: u64) {
        <error descr="'a' is not callable">a</error>();
    }
}        
    """)
}
