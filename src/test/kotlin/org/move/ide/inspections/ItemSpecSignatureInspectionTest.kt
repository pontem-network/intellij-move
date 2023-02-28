package org.move.ide.inspections

import org.move.utils.tests.annotation.InspectionTestBase

class ItemSpecSignatureInspectionTest : InspectionTestBase(ItemSpecSignatureInspection::class) {
    fun `test no error if no signature provided`() = checkByText("""
module 0x1::mod {
    fun call() {}
    spec call {}
}        
    """)

    fun `test no error if correct empty signature`() = checkByText("""
module 0x1::mod {
    fun call() {}
    spec call() {}
}        
    """)

    fun `test no error if correct non-empty signature`() = checkByText("""
module 0x1::mod {
    fun call(account: &signer) {}
    spec call(account: &signer) {}
}        
    """)

    fun `test bounds correctness is not required`() = checkByText("""
module 0x1::m {
    fun max<T: drop + copy + store>() {}
    spec max<T: drop + store>() {}
}
    """)

    fun `test error missing parameter`() = checkByText("""
module 0x1::mod {
    fun call(account: &signer) {}
    spec <warning descr="Function signature mismatch">call()</warning> {}
}        
    """)

    fun `test error missing type parameter`() = checkByText("""
module 0x1::mod {
    fun call<T>() {}
    spec <warning descr="Function signature mismatch">call()</warning> {}
}        
    """)

//    fun `test error missing return type`() = checkByText("""
//module 0x1::mod {
//    fun call(): u64 {}
//    spec <warning descr="Function signature mismatch">call()</warning> {}
//}
//    """)
//
//    fun `test error extra return type`() = checkByText("""
//module 0x1::mod {
//    fun call() {}
//    spec <warning descr="Function signature mismatch">call(): u64</warning> {}
//}
//    """)
}
