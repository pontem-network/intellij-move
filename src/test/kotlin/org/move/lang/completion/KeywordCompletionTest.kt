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
        listOf("resource", "struct", "public", "fun", "const", "use", "native", "spec")
    )

    fun `test space after declaration`() = doSingleCompletion("""
            module M { pub/*caret*/ }
        """, """
            module M { public /*caret*/ }
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

    fun `test public`() = doSingleCompletion("""
        module M {
            pub/*caret*/
        }
    """, """
        module M {
            public /*caret*/
        }
    """)

    fun `test public with other function`() = doSingleCompletion("""
        module M {
            pub/*caret*/
            
            public fun main() {}
        }
    """, """
        module M {
            public /*caret*/
            
            public fun main() {}
        }
    """)

    fun `test public before fun`() = doSingleCompletion("""
        module M {
            pub/*caret*/ fun main() {}
        }
    """, """
        module M {
            public/*caret*/ fun main() {}
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
            native f/*caret*/
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

    fun `test resource before struct`() = doSingleCompletion("""
        module M {
            res/*caret*/ struct
        }
    """, """
        module M {
            resource/*caret*/ struct
        }
    """)

    fun `test no completion in bound if no colon`() = checkNoCompletion("""
        module M {
            struct MyStruct<T cop/*caret*/> {}
        }
    """)

    fun `test copyable bound`() = doSingleCompletion("""
        module M {
            struct MyStruct<T: cop/*caret*/> {}
        }
    """, """
        module M {
            struct MyStruct<T: copyable/*caret*/> {}
        }
    """)

    fun `test resource bound`() = doSingleCompletion("""
        module M {
            struct MyStruct<T: res/*caret*/> {}
        }
    """, """
        module M {
            struct MyStruct<T: resource/*caret*/> {}
        }
    """)

    fun `test acquires keyword`() = doSingleCompletion("""
        module M {
            fun main() acq/*caret*/ {}
        }
    """, """
        module M {
            fun main() acquires /*caret*/ {}
        }
    """)

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
}