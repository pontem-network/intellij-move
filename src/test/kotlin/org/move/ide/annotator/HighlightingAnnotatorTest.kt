package org.move.ide.annotator

import org.move.ide.colors.MvColor
import org.move.utils.tests.MoveV2
import org.move.utils.tests.annotation.AnnotatorTestCase

class HighlightingAnnotatorTest: AnnotatorTestCase(HighlightingAnnotator::class) {
    override fun setUp() {
        super.setUp()
        annotationFixture.registerSeverities(MvColor.values().map(MvColor::testSeverity))
    }

    fun `test block comment do not break the highlighting`() = checkHighlighting(
        """
    /* module M */
    module <MODULE>M</MODULE> {
        fun <FUNCTION>call</FUNCTION>() {}
    }
    """
    )

    fun `test function calls annotated`() = checkHighlighting(
        """
    module 0x1::string {
        public fun mycall() {}
    }    
    module M {
        use 0x1::string::mycall as mycall_alias;
        
        fun <FUNCTION>call</FUNCTION>(x: <PRIMITIVE_TYPE>u8</PRIMITIVE_TYPE>) {}
        fun <FUNCTION>main</FUNCTION>() {
            <FUNCTION_CALL>call</FUNCTION_CALL>(1)
            <FUNCTION_CALL>mycall_alias</FUNCTION_CALL>()
        }
    }    
    """
    )

    fun `test variable names annotated`() = checkHighlighting(
        """
    module <MODULE>M</MODULE> {
        const <CONSTANT>MAX_INT</CONSTANT>: <PRIMITIVE_TYPE>u8</PRIMITIVE_TYPE> = 255;

        fun <FUNCTION>main</FUNCTION>() {
            let a = 1;
            <VARIABLE>a</VARIABLE>;
            <CONSTANT>MAX_INT</CONSTANT>;
        }
    }    
    """
    )

    fun `test types highlighed`() = checkHighlighting(
        """
        module 0x1::M {
            spec schema SS {
                val: <PRIMITIVE_TYPE>num</PRIMITIVE_TYPE>;    
            }
        }
        script {
            fun main(s: &<BUILTIN_TYPE>signer</BUILTIN_TYPE>,
                     val: <PRIMITIVE_TYPE>u8</PRIMITIVE_TYPE>,
                     val2: <PRIMITIVE_TYPE>u64</PRIMITIVE_TYPE>,
                     val3: <PRIMITIVE_TYPE>u128</PRIMITIVE_TYPE>,
                     val4: <PRIMITIVE_TYPE>u16</PRIMITIVE_TYPE>,
                     val5: <PRIMITIVE_TYPE>u32</PRIMITIVE_TYPE>,
                     val6: <PRIMITIVE_TYPE>u256</PRIMITIVE_TYPE>,
                     val7: <PRIMITIVE_TYPE>bool</PRIMITIVE_TYPE>,
                     val8: 0x0::Transaction::bool,
                     ) {
                        let mysigner: <BUILTIN_TYPE>signer</BUILTIN_TYPE>;
                        let signer = 1;
                     }
        }
    """
    )

    fun `test builtin functions highlighted in call positions`() = checkHighlighting(
        """
        module 0x1::m {
            fun move_to() {
                let move_to = 1;
                move_to();
                <BUILTIN_FUNCTION_CALL>move_from</BUILTIN_FUNCTION_CALL>();
                <BUILTIN_FUNCTION_CALL>borrow_global</BUILTIN_FUNCTION_CALL>();
                <BUILTIN_FUNCTION_CALL>borrow_global_mut</BUILTIN_FUNCTION_CALL>();
                <BUILTIN_FUNCTION_CALL>exists</BUILTIN_FUNCTION_CALL>();
                <BUILTIN_FUNCTION_CALL>freeze</BUILTIN_FUNCTION_CALL>();
            }
            spec move_to {
                <BUILTIN_FUNCTION_CALL>global</BUILTIN_FUNCTION_CALL>();
            }
        }
    """
    )

    fun `test highlight view functions`() = checkHighlighting(
        """
        module 0x1::m {
            #[view]
            public fun <VIEW_FUNCTION>get_pool</VIEW_FUNCTION>() {}
            fun main() {
                <VIEW_FUNCTION_CALL>get_pool</VIEW_FUNCTION_CALL>();
            }
        }        
    """
    )

    fun `test highlight entry functions`() = checkHighlighting(
        """
        module 0x1::m {
            public entry fun <ENTRY_FUNCTION>get_pool</ENTRY_FUNCTION>() {}
            fun main() {
                <ENTRY_FUNCTION_CALL>get_pool</ENTRY_FUNCTION_CALL>();
            }
        }        
    """
    )

    fun `test highlight inline functions`() = checkHighlighting(
        """
        module 0x1::m {
            public inline fun <INLINE_FUNCTION>get_pool</INLINE_FUNCTION>() {}
            fun main() {
                <INLINE_FUNCTION_CALL>get_pool</INLINE_FUNCTION_CALL>();
            }
        }        
    """
    )

    fun `test function param named as builtin type`() = checkHighlighting(
        """
        script {
            fun main(signer: &<BUILTIN_TYPE>signer</BUILTIN_TYPE>) {
                Signer::address_of(signer)
            }
        }
    """
    )

    fun `test vector literal`() = checkHighlighting(
        """
        module 0x1::main {
            fun call() {
                <VECTOR_LITERAL>vector</VECTOR_LITERAL>[];
            }            
        }
        """
    )

    fun `test generic type parameters highlighted`() = checkHighlighting(
        """
        module 0x1::M {
            struct MyStruct<<TYPE_PARAMETER>T</TYPE_PARAMETER>> {
                field: <TYPE_PARAMETER>T</TYPE_PARAMETER>
            }
            
            fun main<<TYPE_PARAMETER>U</TYPE_PARAMETER>, <TYPE_PARAMETER>V</TYPE_PARAMETER>>(
                a: <TYPE_PARAMETER>U</TYPE_PARAMETER>, 
                b: <TYPE_PARAMETER>V</TYPE_PARAMETER>
            ): <TYPE_PARAMETER>U</TYPE_PARAMETER>
            acquires <TYPE_PARAMETER>U</TYPE_PARAMETER> {
                let a: <TYPE_PARAMETER>U</TYPE_PARAMETER> = 1;
            }
            spec main<<TYPE_PARAMETER>U</TYPE_PARAMETER>, <TYPE_PARAMETER>V</TYPE_PARAMETER>>() {}
            spec schema MySchema<<TYPE_PARAMETER>Type</TYPE_PARAMETER>> {}
            spec module {
                apply MySchema<<TYPE_PARAMETER>Type</TYPE_PARAMETER>> to *<<TYPE_PARAMETER>Type</TYPE_PARAMETER>>;
                native fun serialize<<TYPE_PARAMETER>U</TYPE_PARAMETER>>(v: &<TYPE_PARAMETER>U</TYPE_PARAMETER>): vector<u8>;
            }
        }
    """
    )

    fun `test Self as keyword`() = checkHighlighting(
        """
    module 0x1::m {
        use 0x1::Transaction::<KEYWORD>Self</KEYWORD>;
        fun main() {
            <KEYWORD>Self</KEYWORD>::call();
        }
    }
        """
    )

    fun `test copy is keyword`() = checkHighlighting(
        """
    module 0x1::m {
        struct S has copy {}
        fun main() {
            let a = <KEYWORD>copy</KEYWORD> 1;
        }
    }
        """
    )

    fun `test spec keywords are highlighted`() = checkHighlighting(
        """
    module M {
        spec module {
            <KEYWORD>assume</KEYWORD> 1 == 1;
            <KEYWORD>assert</KEYWORD> 1 == 1;
        }
    }    
    """
    )

    fun `test address highlighting`() = checkHighlighting(
        """
    module 0x1::M {
        #[test(acc = <ADDRESS>@0x42</ADDRESS>, acc2 = <ADDRESS>@0x43</ADDRESS>)]
        fun m() {
            <ADDRESS>@DiemRoot</ADDRESS>;
            borrow_global_mut<address>(<ADDRESS>@0x1</ADDRESS>)
        }
    }
    """
    )

    fun `test macros are highlighted`() = checkHighlighting(
        """
    module 0x1::M {
        fun m() {
            <MACRO>assert</MACRO><MACRO>!</MACRO>(true, 1);
        }
    }
    """
    )

    fun `test integer highlighting`() = checkHighlighting(
        """
    module 0x1::main {
        fun main() {
            let a = <NUMBER>0x123456</NUMBER>;
            
        }
    }
    """
    )

    fun `test structs and struct fields are highlighted`() = checkHighlighting(
        """
        module 0x1::m {
            struct <STRUCT>MyS</STRUCT> {
                <FIELD>field1</FIELD>: u8,
                <FIELD>field2</FIELD>: u8,
            }
            fun main(s: <STRUCT>MyS</STRUCT>): <STRUCT>MyS</STRUCT> {
                s.<FIELD>field1</FIELD>;
                s.<FIELD>field2</FIELD>;
                let <STRUCT>MyS</STRUCT> { <FIELD>field1</FIELD>: myfield1, <FIELD>field2</FIELD>: myfield2 } = s;
                <STRUCT>MyS</STRUCT> { <FIELD>field1</FIELD>: 1, <FIELD>field2</FIELD>: 2 }
            }
        }
    """
    )

    fun `test highlight objects`() = checkHighlighting(
        """
        module 0x1::m {
            struct Res {}
            struct ResKey has key {}
            struct ResStoreDrop has store, drop {}
            struct ResStore has store {}
            fun objects(
                <VARIABLE>binding</VARIABLE>: u8,
                <KEY_OBJECT>res_key</KEY_OBJECT>: ResKey, 
                <STORE_OBJECT>res_store_drop</STORE_OBJECT>: ResStoreDrop, 
                <STORE_NO_DROP_OBJECT>res_store</STORE_NO_DROP_OBJECT>: ResStore, 
            ) {
                <KEY_OBJECT>res_key</KEY_OBJECT>;
                <STORE_OBJECT>res_store_drop</STORE_OBJECT>;
                <STORE_NO_DROP_OBJECT>res_store</STORE_NO_DROP_OBJECT>;
            }
        }        
    """
    )

    fun `test highlight references`() = checkHighlighting(
        """
        module 0x1::m {
            struct Res {}
            struct ResKey has key {}
            struct ResStoreDrop has store, drop {}
            struct ResStoreNoDrop has store {}
            fun refs(<REF>ref_res</REF>: &Res, <MUT_REF>mut_ref_res</MUT_REF>: &mut Res) {
                <REF>ref_res</REF>;
                <MUT_REF>mut_ref_res</MUT_REF>;
            }
            fun ref_to_object(
                <REF_TO_KEY_OBJECT>ref_res_key</REF_TO_KEY_OBJECT>: &ResKey, 
                <REF_TO_STORE_OBJECT>ref_res_store_drop</REF_TO_STORE_OBJECT>: &ResStoreDrop, 
                <REF_TO_STORE_NO_DROP_OBJECT>ref_res_store_no_drop</REF_TO_STORE_NO_DROP_OBJECT>: &ResStoreNoDrop, 
                ) {
                <REF_TO_KEY_OBJECT>ref_res_key</REF_TO_KEY_OBJECT>;
                <REF_TO_STORE_OBJECT>ref_res_store_drop</REF_TO_STORE_OBJECT>;
                <REF_TO_STORE_NO_DROP_OBJECT>ref_res_store_no_drop</REF_TO_STORE_NO_DROP_OBJECT>;
            }
            fun mut_ref_to_object(
                <MUT_REF_TO_KEY_OBJECT>ref_res_key</MUT_REF_TO_KEY_OBJECT>: &mut ResKey, 
                <MUT_REF_TO_STORE_OBJECT>ref_res_store_drop</MUT_REF_TO_STORE_OBJECT>: &mut ResStoreDrop, 
                <MUT_REF_TO_STORE_NO_DROP_OBJECT>ref_res_store_no_drop</MUT_REF_TO_STORE_NO_DROP_OBJECT>: &mut ResStoreNoDrop, 
                ) {
                <MUT_REF_TO_KEY_OBJECT>ref_res_key</MUT_REF_TO_KEY_OBJECT>;
                <MUT_REF_TO_STORE_OBJECT>ref_res_store_drop</MUT_REF_TO_STORE_OBJECT>;
                <MUT_REF_TO_STORE_NO_DROP_OBJECT>ref_res_store_no_drop</MUT_REF_TO_STORE_NO_DROP_OBJECT>;
            }
        }        
    """
    )

    fun `test for loop keywords`() = checkHighlighting(
        """
        module 0x1::m {
            fun main() {
                let <VARIABLE>for</VARIABLE> = 1;
                <KEYWORD>for</KEYWORD> (i <KEYWORD>in</KEYWORD> 1..10) {};
            }
        }        
    """
    )

    @MoveV2()
    fun `test highlight methods`() = checkHighlighting(
        """
        module 0x1::m {
            struct S { field: u8 }
            fun <METHOD>receiver</METHOD>(<SELF_PARAMETER>self</SELF_PARAMETER>: S): u8 { 
                <SELF_PARAMETER>self</SELF_PARAMETER>.field 
            }
            fun main(s: S, <VARIABLE>self</VARIABLE>: u8) {
                s.<METHOD_CALL>receiver</METHOD_CALL>();
            }
        }        
    """
    )

    fun `test do not highlight methods if compiler v1`() = checkHighlighting(
        """
        module 0x1::m {
            struct S { field: u8 }
            fun <FUNCTION>receiver</FUNCTION>(<VARIABLE>self</VARIABLE>: S): u8 { 
                <VARIABLE>self</VARIABLE>.field 
            }
            fun main(s: S, <VARIABLE>self</VARIABLE>: u8) {
                s.<METHOD_CALL>receiver</METHOD_CALL>();
            }
        }        
    """
    )

    fun `test enum highlighting`() = checkHighlighting("""
        module 0x1::m {
            <KEYWORD>enum</KEYWORD> <ENUM>S</ENUM> { One, Two(u8) }
        }        
    """)

    fun `test attribute highlighting`() = checkHighlighting("""
        module 0x1::m {
            <ATTRIBUTE>#</ATTRIBUTE><ATTRIBUTE>[</ATTRIBUTE><ATTRIBUTE>view</ATTRIBUTE><ATTRIBUTE>]</ATTRIBUTE>
            fun foo() {}
        }        
    """)

//    fun `test resource access control keywords highlighting`() = checkHighlighting(
//        """
//        module 0x1::m {
//            fun f_multiple() <KEYWORD>reads</KEYWORD> R <KEYWORD>writes</KEYWORD> T, S <KEYWORD>reads</KEYWORD> G<u64> {}
//            fun f_multiple2() <KEYWORD>pure</KEYWORD> {}
//        }
//    """
//    )
}
