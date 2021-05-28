package org.move.lang.completion

import org.move.utils.tests.completion.CompletionTestCase

class KeywordCompletionTest : CompletionTestCase() {
    fun `test address`() = doSingleCompletion("""
        addr/*caret*/    
    """, """
        address /*caret*/    
    """)

    fun `test top level module`() = doSingleCompletion("""
        mod/*caret*/    
    """, """
        module /*caret*/    
    """)

//    fun `test script keyword completion if brace is present`() = doSingleCompletion("""
//        scr/*caret*/ {}
//    """, """
//        script/*caret*/ {}
//    """)

    fun `test fun cannot be top-level`() = checkNoCompletion("""
        fu/*caret*/    
    """)

    fun `test fun cannot be in address`() = checkNoCompletion("""
        address 0x0 {
            fu/*caret*/
        }
    """)

    fun `test module in address`() = doSingleCompletion("""
       address 0x0 {
           mod/*caret*/     
       } 
    """, """
       address 0x0 {
           module /*caret*/     
       } 
    """)

    fun `test only module keyword in address`() = completionFixture.checkNotContainsCompletion(
        """ address 0x0 { /*caret*/ }""",
        listOf("address", "script", "let", "fun")
    )

    fun `test top level module declarations`() = completionFixture.checkContainsCompletion(
        """module M { /*caret*/ }""",
        listOf("resource", "struct", "public", "fun", "const", "use", "friend", "native", "spec")
    )

    fun `test space after declaration`() = doSingleCompletion("""
            module M { fu/*caret*/ }
        """, """
            module M { fun /*caret*/ }
        """)

    fun `test no completion in address literal`() = checkNoCompletion(
        " address /*caret*/ {} "
    )

    fun `test no completion in module name`() = checkNoCompletion("""
            module /*caret*/ {} 
        """)

    fun `test function let completion`() = doFirstCompletion("""
            script { 
                fun main() {
                    le/*caret*/ 
                }
            } 
        """, """
            script { 
                fun main() {
                    let /*caret*/ 
                }
            } 
        """
    )

    fun `test no completion in let name`() = checkNoCompletion("""
            script { 
                fun main() {
                    let name = 1;
                    let n/*caret*/ 
                }
            } 
        """)

    fun `test spec`() = doSingleCompletion("""
        module M {
            sp/*caret*/
        }
    """, """
        module M {
            spec /*caret*/
        }
    """)

    fun `test spec fun`() = doSingleCompletion("""
        module M {
            spec f/*caret*/
        }
    """, """
        module M {
            spec fun /*caret*/
        }
    """)

    fun `test spec struct`() = doSingleCompletion("""
        module M {
            spec st/*caret*/
        }
    """, """
        module M {
            spec struct /*caret*/
        }
    """)

    fun `test spec module`() = doSingleCompletion("""
        module M {
            spec mod/*caret*/
        }
    """, """
        module M {
            spec module /*caret*/
        }
    """)

    fun `test spec schema`() = doSingleCompletion("""
        module M {
            spec sch/*caret*/
        }
    """, """
        module M {
            spec schema /*caret*/
        }
    """)

    fun `test public`() = completionFixture.checkContainsCompletion("""
        module M {
            pub/*caret*/
        }
    """, listOf("public", "public(script)", "public(friend)"))

    fun `test public with other function`() = checkContainsCompletion("public","""
        module M {
            pub/*caret*/
            
            public fun main() {}
        }
    """)

    fun `test public before fun`() = checkContainsCompletion("public", """
        module M {
            pub/*caret*/ fun main() {}
        }
    """)

    fun `test native before fun`() = doSingleCompletion("""
        module M {
            nat/*caret*/ fun main() {}
        }
    """, """
        module M {
            native/*caret*/ fun main() {}
        }
    """)

    fun `test native fun to public`() = checkContainsCompletion("public","""
        module M {
            native pub/*caret*/ fun main();
        }
    """)

    fun `test public fun`() = doSingleCompletion("""
        module M {
            public f/*caret*/
        }
    """, """
        module M {
            public fun /*caret*/
        }
    """)

    fun `test native fun`() = doSingleCompletion("""
        module M {
            native fu/*caret*/
        }
    """, """
        module M {
            native fun /*caret*/
        }
    """)

    fun `test native public fun`() = doSingleCompletion("""
        module M {
            native public f/*caret*/
        }
    """, """
        module M {
            native public fun /*caret*/
        }
    """)

    fun `test resource`() = doSingleCompletion("""
        module M {
            reso/*caret*/
        }
    """, """
        module M {
            resource /*caret*/
        }
    """)

    fun `test resource struct`() = doSingleCompletion("""
        module M {
            resource st/*caret*/
        }
    """, """
        module M {
            resource struct /*caret*/
        }
    """)

    fun `test no completion in bound if no colon`() = checkNoCompletion("""
        module M {
            struct MyStruct<T cop/*caret*/> {}
        }
    """)

    fun `test copy bound`() = doSingleCompletion("""
        module M {
            struct MyStruct<T: cop/*caret*/> {}
        }
    """, """
        module M {
            struct MyStruct<T: copy/*caret*/> {}
        }
    """)

    fun `test store bound`() = doSingleCompletion("""
        module M {
            struct MyStruct<T: st/*caret*/> {}
        }
    """, """
        module M {
            struct MyStruct<T: store/*caret*/> {}
        }
    """)

    fun `test resource bound`() = checkNoCompletion("""
        module M {
            struct MyStruct<T: res/*caret*/> {}
        }
    """)

//    fun `test acquires keyword`() = doSingleCompletion("""
//        module M {
//            fun main() acq/*caret*/ {}
//        }
//    """, """
//        module M {
//            fun main() acquires /*caret*/ {}
//        }
//    """)
//
//    fun `test acquires after return type`() = doSingleCompletion("""
//        module M {
//            fun main(): u8 acq/*caret*/ {}
//        }
//    """, """
//        module M {
//            fun main(): u8 acquires /*caret*/ {}
//        }
//    """)

    fun `test no acquires after fun keyword`() = checkNoCompletion("""
        module M {
            fun acq/*caret*/ {}
        }
    """)

    fun `test no acquires inside param list`() = checkNoCompletion("""
        module M {
            fun main(acq/*caret*/ {}
        }
    """)

    fun `test no acquires inside return type`() = checkNoCompletion("""
        module M {
            fun main(): acq/*caret*/ {}
        }
    """)

    fun `test native struct`() = doSingleCompletion("""
        module M {
            native stru/*caret*/
        }
    """, """
        module M {
            native struct /*caret*/
        }
    """)

    fun `test visibility modifiers`() = completionFixture.checkContainsCompletion("""
       module M {
        pub/*caret*/ fun main() {}
       }    
    """, listOf("public", "public(script)", "public(friend)"))

    fun `test public(script) without leading fun adds fun`() = doSingleCompletion("""
    module M {
        public(scr/*caret*/
    }
    """, """
    module M {
        public(script) fun /*caret*/
    }
    """)

    fun `test public(script) with leading fun adds just modifier`() = doSingleCompletion("""
    module M {
        public(scr/*caret*/ fun
    }
    """, """
    module M {
        public(script)/*caret*/ fun 
    }
    """)
}
