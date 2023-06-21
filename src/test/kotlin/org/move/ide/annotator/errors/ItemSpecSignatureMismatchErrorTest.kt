package org.move.ide.annotator.errors

import org.move.ide.annotator.MvErrorAnnotator
import org.move.utils.tests.annotation.AnnotatorTestCase

class ItemSpecSignatureMismatchErrorTest : AnnotatorTestCase(MvErrorAnnotator::class) {
    fun `test no error if no signature provided`() = checkWarnings(
        """
module 0x1::mod {
    fun call() {}
    spec call {}
}        
    """
    )

    fun `test no error if correct empty signature`() = checkWarnings(
        """
module 0x1::mod {
    fun call() {}
    spec call() {}
}        
    """
    )

    fun `test no error if correct non-empty signature`() = checkWarnings(
        """
module 0x1::mod {
    fun call(account: &signer) {}
    spec call(account: &signer) {}
}        
    """
    )

    fun `test bounds correctness is not required`() = checkWarnings(
        """
module 0x1::m {
    fun max<T: drop + copy + store>() {}
    spec max<T: drop + store>() {}
}
    """
    )

    fun `test error missing parameter`() = checkWarnings(
        """
module 0x1::mod {
    fun call(account: &signer) {}
    spec <warning descr="Function signature mismatch">call()</warning> {}
}        
    """
    )

    fun `test error missing type parameter`() = checkWarnings(
        """
module 0x1::mod {
    fun call<T>() {}
    spec <warning descr="Function signature mismatch">call()</warning> {}
}        
    """
    )

    fun `test error missing return type`() = checkWarnings(
        """
module 0x1::mod {
    fun call(): u64 {}
    spec <warning descr="Function signature mismatch">call()</warning> {}
}
    """
    )

    fun `test error extra return type`() = checkWarnings(
        """
module 0x1::mod {
    fun call() {}
    spec <warning descr="Function signature mismatch">call(): u64</warning> {}
}
    """
    )

    // TODO: test
//    fun `test no mismatch if type is passed with a different qual name`() = checkWarnings("""
//        module 0x1::string {
//            struct String {}
//        }
//        module 0x1::m {
//            use 0x1::string::{Self, String};
//            fun call(): String {}
//            spec call(): string::String {}
//        }
//    """)
}
