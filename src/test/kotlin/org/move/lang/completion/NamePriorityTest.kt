package org.move.lang.completion

import org.move.utils.tests.completion.CompletionTestCase

class NamePriorityTest: CompletionTestCase() {
    fun `test builtin has more priority than unimported function`() = doFirstCompletion("""
    module 0x1::M1 {
        public fun borrow() {}
    }    
    module 0x1::M2 {
        fun call() {
            borro/*caret*/
        }
    }
    """, """
    module 0x1::M1 {
        public fun borrow() {}
    }    
    module 0x1::M2 {
        fun call() {
            borrow_global</*caret*/>()
        }
    }
    """)

    fun `test local has more priority than unimported function`() = doFirstCompletion("""
    module 0x1::M1 {
        public fun call_a() {}
    }    
    module 0x1::M2 {
        fun call_z() {}
        fun main() {
            cal/*caret*/
        }
    }
    """, """
    module 0x1::M1 {
        public fun call_a() {}
    }    
    module 0x1::M2 {
        fun call_z() {}
        fun main() {
            call_z()/*caret*/
        }
    }
    """)

//    fun `test sort by expected type`() = doFirstCompletion("""
//    module 0x1::M2 {
//        fun call(a: &signer) {}
//        fun main(val_a: address, val_z: &signer) {
//            call(val/*caret*/)
//        }
//    }
//    """, """
//    module 0x1::M2 {
//        fun call(a: &signer) {}
//        fun main(val_a: address, val_z: &signer) {
//            call(val_z/*caret*/)
//        }
//    }
//    """)
}
