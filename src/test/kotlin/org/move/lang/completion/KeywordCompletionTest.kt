package org.move.lang.completion

import org.move.ide.inspections.fixes.CompilerV2Feat.*
import org.move.utils.tests.CompilerV2Features
import org.move.utils.tests.completion.CompletionTestCase

class KeywordCompletionTest: CompletionTestCase() {
    fun `test address`() = doSingleCompletion(
        """
        addr/*caret*/    
    """, """
        address /*caret*/    
    """
    )

    fun `test top level module`() = doSingleCompletion(
        """
        mod/*caret*/    
    """, """
        module /*caret*/    
    """
    )

    fun `test top level spec`() = doSingleCompletion(
        """
        sp/*caret*/    
    """, """
        spec /*caret*/    
    """
    )

//    fun `test script keyword completion if brace is present`() = doSingleCompletion("""
//        scr/*caret*/ {}
//    """, """
//        script/*caret*/ {}
//    """)

    fun `test fun cannot be top-level`() = checkNoCompletion(
        """
        fu/*caret*/    
    """
    )

    fun `test fun cannot be in address`() = checkNoCompletion(
        """
        address 0x0 {
            fu/*caret*/
        }
    """
    )

    fun `test module in address`() = doSingleCompletion(
        """
       address 0x0 {
           mod/*caret*/     
       } 
    """, """
       address 0x0 {
           module /*caret*/     
       } 
    """
    )

    fun `test only module keyword in address`() = completionFixture.checkNotContainsCompletion(
        """ address 0x0 { /*caret*/ }""",
        listOf("address", "script", "let", "fun")
    )

    fun `test top level module declarations`() = completionFixture.checkContainsCompletion(
        """module 0x1::M { /*caret*/ }""",
        listOf("struct", "public", "fun", "const", "use", "friend", "native", "spec")
    )

    fun `test space after declaration`() = doSingleCompletion(
        """
            module 0x1::M { fu/*caret*/ }
        """, """
            module 0x1::M { fun /*caret*/ }
        """
    )

    fun `test no completion in address literal`() = checkNoCompletion(
        " address /*caret*/ {} "
    )

    fun `test no completion in module name`() = checkNoCompletion(
        """
            module /*caret*/ {} 
        """
    )

    fun `test function let completion`() = doFirstCompletion(
        """
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

    fun `test no completion in let name`() = checkNoCompletion(
        """
            script { 
                fun main() {
                    let name = 1;
                    let n/*caret*/ 
                }
            } 
        """
    )

    fun `test spec`() = doSingleCompletion(
        """
        module 0x1::M {
            sp/*caret*/
        }
    """, """
        module 0x1::M {
            spec /*caret*/
        }
    """
    )

    fun `test spec in spec module`() = doSingleCompletion(
        """
        spec 0x1::M {
            sp/*caret*/
        }
    """, """
        spec 0x1::M {
            spec /*caret*/
        }
    """
    )

    fun `test spec fun`() = doSingleCompletion(
        """
        module 0x1::M {
            spec f/*caret*/
        }
    """, """
        module 0x1::M {
            spec fun /*caret*/
        }
    """
    )

    fun `test spec module`() = doSingleCompletion(
        """
        module 0x1::M {
            spec mod/*caret*/
        }
    """, """
        module 0x1::M {
            spec module /*caret*/
        }
    """
    )

    fun `test spec schema`() = doSingleCompletion(
        """
        module 0x1::M {
            spec sch/*caret*/
        }
    """, """
        module 0x1::M {
            spec schema /*caret*/
        }
    """
    )

    @CompilerV2Features(PUBLIC_PACKAGE)
    fun `test public`() = completionFixture.checkContainsCompletion(
        """
        module 0x1::M {
            pub/*caret*/
        }
    """, listOf("public", "public(script)", "public(friend)", "public(package)")
    )

    fun `test public with other function`() = checkContainsCompletion(
        "public", """
        module 0x1::M {
            pub/*caret*/
            
            public fun main() {}
        }
    """
    )

    fun `test public after end of function`() = checkContainsCompletion(
        "public", """
        module 0x1::M { 
            public fun main() {}
            pub/*caret*/
        }
    """
    )

    fun `test public before fun`() = checkContainsCompletion(
        "public", """
        module 0x1::M {
            pub/*caret*/ fun main() {}
        }
    """
    )

    fun `test native before fun`() = doSingleCompletion(
        """
        module 0x1::M {
            nat/*caret*/ fun main() {}
        }
    """, """
        module 0x1::M {
            native/*caret*/ fun main() {}
        }
    """
    )

    fun `test native fun to public`() = checkContainsCompletion(
        "public", """
        module 0x1::M {
            native pub/*caret*/ fun main();
        }
    """
    )

    fun `test public fun`() = doSingleCompletion(
        """
        module 0x1::M {
            public f/*caret*/
        }
    """, """
        module 0x1::M {
            public fun /*caret*/
        }
    """
    )

    fun `test public(script) fun`() = doSingleCompletion(
        """
        module 0x1::M {
            public(script) f/*caret*/
        }
    """, """
        module 0x1::M {
            public(script) fun /*caret*/
        }
    """
    )

    fun `test native fun`() = doSingleCompletion(
        """
        module 0x1::M {
            native fu/*caret*/
        }
    """, """
        module 0x1::M {
            native fun /*caret*/
        }
    """
    )

    fun `test native public fun`() = doSingleCompletion(
        """
        module 0x1::M {
            native public f/*caret*/
        }
    """, """
        module 0x1::M {
            native public fun /*caret*/
        }
    """
    )

    fun `test entry`() = doSingleCompletion(
        """
        module 0x1::M {
            ent/*caret*/
        }
    """, """
        module 0x1::M {
            entry /*caret*/
        }
    """
    )

    fun `test entry after public`() = doSingleCompletion(
        """
        module 0x1::M {
            public ent/*caret*/
        }
    """, """
        module 0x1::M {
            public entry /*caret*/
        }
    """
    )

    fun `test entry after public friend`() = doSingleCompletion(
        """
        module 0x1::M {
            public(friend) ent/*caret*/
        }
    """, """
        module 0x1::M {
            public(friend) entry /*caret*/
        }
    """
    )

    fun `test no completion in bound if no colon`() = checkNoCompletion(
        """
        module 0x1::M {
            struct MyStruct<T cop/*caret*/> {}
        }
    """
    )

    fun `test copy bound`() = doSingleCompletion(
        """
        module 0x1::M {
            struct MyStruct<T: cop/*caret*/> {}
        }
    """, """
        module 0x1::M {
            struct MyStruct<T: copy/*caret*/> {}
        }
    """
    )

    fun `test store bound`() = doSingleCompletion(
        """
        module 0x1::M {
            struct MyStruct<T: st/*caret*/> {}
        }
    """, """
        module 0x1::M {
            struct MyStruct<T: store/*caret*/> {}
        }
    """
    )

    fun `test resource bound`() = checkNoCompletion(
        """
        module 0x1::M {
            struct MyStruct<T: res/*caret*/> {}
        }
    """
    )

    fun `test acquires keyword`() = doSingleCompletion(
        """
        module 0x1::M {
            fun main() acq/*caret*/{}
        }
    """, """
        module 0x1::M {
            fun main() acquires /*caret*/ {}
        }
    """
    )

    fun `test acquires keyword with space`() = doSingleCompletion(
        """
        module 0x1::M {
            fun main() acq/*caret*/ {}
        }
    """, """
        module 0x1::M {
            fun main() acquires /*caret*/ {}
        }
    """
    )

    //
    fun `test acquires after return type`() = doSingleCompletion(
        """
        module 0x1::M {
            fun main(): u8 acq/*caret*/ {}
        }
    """, """
        module 0x1::M {
            fun main(): u8 acquires /*caret*/ {}
        }
    """
    )

    fun `test acquires after reference return type`() = doSingleCompletion(
        """
        module 0x1::M {
            fun main(): &u8 acq/*caret*/ {}
        }
    """, """
        module 0x1::M {
            fun main(): &u8 acquires /*caret*/ {}
        }
    """
    )

    fun `test no acquires after colon`() = checkNoCompletion(
        """
        module 0x1::M {
            fun call(): acq/*caret*/ {}
        }
    """
    )

    fun `test no acquires after fun keyword`() = checkNoCompletion(
        """
        module 0x1::M {
            fun acq/*caret*/ {}
        }
    """
    )

    fun `test no acquires inside param list`() = checkNoCompletion(
        """
        module 0x1::M {
            fun main(acq/*caret*/ {}
        }
    """
    )

    fun `test no acquires inside return type`() = checkNoCompletion(
        """
        module 0x1::M {
            fun main(): acq/*caret*/ {}
        }
    """
    )

    fun `test native struct`() = doSingleCompletion(
        """
        module 0x1::M {
            native stru/*caret*/
        }
    """, """
        module 0x1::M {
            native struct /*caret*/
        }
    """
    )

    fun `test visibility modifiers compiler v1`() = completionFixture.checkContainsCompletion(
        """
       module 0x1::M {
        pub/*caret*/ fun main() {}
       }    
    """, listOf("public", "public(script)", "public(friend)")
    )

    @CompilerV2Features(PUBLIC_PACKAGE)
    fun `test visibility modifiers with public package`() = completionFixture.checkContainsCompletion(
        """
       module 0x1::M {
        pub/*caret*/ fun main() {}
       }    
    """, listOf("public", "public(script)", "public(friend)", "public(package)")
    )

//    fun `test public(script) without leading fun adds fun`() = doSingleCompletion("""
//    module 0x1::M {
//        public(scr/*caret*/
//    }
//    """, """
//    module 0x1::M {
//        public(script) fun /*caret*/
//    }
//    """)
//
//    fun `test public(script) with leading fun adds just modifier`() = doSingleCompletion("""
//    module 0x1::M {
//        public(scr/*caret*/ fun
//    }
//    """, """
//    module 0x1::M {
//        public(script)/*caret*/ fun
//    }
//    """)

    fun `test no expression keywords at field name position in struct literal`() = checkNoCompletion(
        """
    module 0x1::M {
        struct S { field: u8 }
        fun m() {
            let s = S { ret/*caret*/ };
        }
    }    
    """
    )

    fun `test phantom keyword in struct generic`() = doSingleCompletion(
        """
    module 0x1::M {
        struct S<ph/*caret*/>
    }    
    """, """
    module 0x1::M {
        struct S<phantom /*caret*/>
    }    
    """
    )

    fun `test phantom keyword in struct generic second param`() = doSingleCompletion(
        """
    module 0x1::M {
        struct S<Type, ph/*caret*/>
    }    
    """, """
    module 0x1::M {
        struct S<Type, phantom /*caret*/>
    }    
    """
    )

    fun `test phantom keyword in struct generic before existing type`() = doSingleCompletion(
        """
    module 0x1::M {
        struct S<ph/*caret*/CoinType>
    }    
    """, """
    module 0x1::M {
        struct S<phantom /*caret*/CoinType>
    }    
    """
    )

    fun `test spec keywords completion`() = doSingleCompletion(
        """
    module 0x1::M {
        spec module {
            incl/*caret*/
        }
    }    
    """, """
    module 0x1::M {
        spec module {
            include /*caret*/
        }
    }    
    """
    )

    fun `test spec keywords completion all`() = checkContainsCompletion(
        listOf(
            "include", "assume", "assert", "requires", "modifies", "ensures", "aborts_if", "aborts_with",
            "invariant", "apply", "let", "use"
        ),
        """
    module 0x1::M {
        spec module {
            /*caret*/
        }
    }    
    """
    )

    fun `test spec module space exists`() = doSingleCompletion(
        """
    module 0x1::M {
        spec mo/*caret*/ {}
    }    
    """, """
    module 0x1::M {
        spec module/*caret*/ {}
    }    
    """
    )

    fun `test else after if block`() = doSingleCompletion(
        """
    module 0x1::M {
        fun call() {
            if (true) {
                
            } el/*caret*/
        }
    }    
    """, """
    module 0x1::M {
        fun call() {
            if (true) {
                
            } else /*caret*/
        }
    }    
    """
    )

    fun `test continue inside while`() = doSingleCompletion(
        """
    module 0x1::Main {
        fun call() {
            while (true) {
                con/*caret*/
            }
        }
    }    
    """, """
    module 0x1::Main {
        fun call() {
            while (true) {
                continue /*caret*/
            }
        }
    }    
    """
    )

//    fun `test bool completion in field initializer`() = doSingleCompletion("""
//module 0x1::main {
//    struct Container<Type> { val: Type }
//    fun main() {
//        Container<bool> { val: fa/*caret*/ };
//    }
//}
//    """, """
//module 0x1::main {
//    struct Container<Type> { val: Type }
//    fun main() {
//        Container<bool> { val: false/*caret*/ };
//    }
//}
//    """)

    fun `test no completion for reads in v1`() = checkNotContainsCompletion(
        "reads",
        """
            module 0x1::m {
                fun main() rea/*caret*/ {}
            }            
        """
    )

    @CompilerV2Features(RESOURCE_CONTROL)
    fun `test completion for resource access modifiers`() = checkContainsCompletion(
        listOf("reads", "writes", "pure", "acquires"),
        """
            module 0x1::m {
                fun main() /*caret*/ {}
            }            
        """
    )
}
