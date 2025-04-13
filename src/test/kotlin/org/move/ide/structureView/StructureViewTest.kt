package org.move.ide.structureView

import org.intellij.lang.annotations.Language

class StructureViewTest: StructureViewTestBase() {
    fun `test address`() = doTest(
        """
address 0x1 {
    module M {
        fun call() {}
    }
}
    """, """
-main.move
 -M
  call()
    """
    )

    fun `test scripts`() = doTest(
        """
    script {
        fun script_fun_1() {}
    }
    script {
        fun script_fun_2() {}
    }
    """, """
-main.move
 script_fun_1()
 script_fun_2()
    """
    )

    fun `test module items`() = doTest(
        """
    module 0x1::m {
        const ERR_NOT_FOUND: u64 = 1;
        const ERR_NOT_ENOUGH_PERMISSIONS: u64 = 2;
        struct Struct1 {
            field1: u8
        }
        struct Struct2 {
            field2: u8
        }
        enum S { One, Two, Three { val: u8 }, Four(u8, u8)}
        public fun call_pub() {}
        public(friend) fun call_pub_friend() {}
        entry fun call_entry() {}
        fun double(i: u8): u8 {}
        fun main() {}
        spec fun find(): u8 {}
    }    
    """, """
-main.move
 -m
  ERR_NOT_FOUND: u64
  ERR_NOT_ENOUGH_PERMISSIONS: u64
  -Struct1
   field1: u8
  -Struct2
   field2: u8
  -S
   One
   Two
   -Three
    val: u8
   Four(u8, u8)
  call_pub()
  call_pub_friend()
  call_entry()
  double(i: u8): u8
  main()
  find(): num
    """
    )

    private fun doTest(@Language("Move") code: String, expected: String) {
        doTestStructureView(code) {
            val normExpected = expected.trimMargin()
            assertTreeEqual(tree, normExpected)
        }
    }
}
