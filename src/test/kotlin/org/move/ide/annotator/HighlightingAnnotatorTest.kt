package org.move.ide.annotator

import org.move.ide.colors.MvColor
import org.move.utils.tests.annotation.AnnotatorTestCase

class HighlightingAnnotatorTest : AnnotatorTestCase(HighlightingAnnotator::class) {
    override fun setUp() {
        super.setUp()
        annotationFixture.registerSeverities(MvColor.values().map(MvColor::testSeverity))
    }

    fun `test block comment do not break the highlighting`() = checkHighlighting("""
    /* module M */
    module <MODULE_DEF>M</MODULE_DEF> {
        fun <FUNCTION_DEF>call</FUNCTION_DEF>() {}
    }
    """)

    fun `test function calls annotated`() = checkHighlighting("""
    module M {
        fun <FUNCTION_DEF>call</FUNCTION_DEF>(x: <PRIMITIVE_TYPE>u8</PRIMITIVE_TYPE>) {}
        fun <FUNCTION_DEF>main</FUNCTION_DEF>() {
            <FUNCTION_CALL>call</FUNCTION_CALL>(1)
        }
    }    
    """)

    fun `test variable names annotated`() = checkHighlighting("""
    module <MODULE_DEF>M</MODULE_DEF> {
        const <CONSTANT_DEF>MAX_INT</CONSTANT_DEF>: <PRIMITIVE_TYPE>u8</PRIMITIVE_TYPE> = 255;

        fun <FUNCTION_DEF>main</FUNCTION_DEF>() {
            let a = 1;
            <VARIABLE>a</VARIABLE>;
            <CONSTANT>MAX_INT</CONSTANT>;
        }
    }    
    """)

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
                     val4: <PRIMITIVE_TYPE>bool</PRIMITIVE_TYPE>,
                     val5: 0x0::Transaction::bool,
                     ) {
                        let mysigner: <BUILTIN_TYPE>signer</BUILTIN_TYPE>;
                        let signer = 1;
                     }
        }
    """
    )

    fun `test builtin functions highlighted in call positions`() = checkHighlighting(
        """
        module M {
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
        module M {
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
            spec schema MySchema<<TYPE_PARAMETER>Type</TYPE_PARAMETER>> {}
            spec module {
                apply MySchema<<TYPE_PARAMETER>Type</TYPE_PARAMETER>> to *<<TYPE_PARAMETER>Type</TYPE_PARAMETER>>;
                native fun serialize<<TYPE_PARAMETER>U</TYPE_PARAMETER>>(v: &<TYPE_PARAMETER>U</TYPE_PARAMETER>): vector<u8>;
            }
        }
    """
    )

    fun `test imported Self as keyword`() = checkHighlighting(
        """
    module M {
        use 0x1::Transaction::<KEYWORD>Self</KEYWORD>
    }
        """
    )

    fun `test copy is keyword`() = checkHighlighting(
        """
    module M {
        struct S has copy {}
        fun main() {
            let a = <KEYWORD>copy</KEYWORD> 1;
        }
    }
        """
    )

    fun `test spec keywords are highlighted`() = checkHighlighting("""
    module M {
        spec module {
            <KEYWORD>assume</KEYWORD> 1 == 1;
            <KEYWORD>assert</KEYWORD> 1 == 1;
        }
    }    
    """)

    fun `test address highlighting`() = checkHighlighting("""
    module 0x1::M {
        #[test(acc = <ADDRESS>@0x42</ADDRESS>, acc2 = <ADDRESS>@0x43</ADDRESS>)]
        fun m() {
            <ADDRESS>@DiemRoot</ADDRESS>;
            borrow_global_mut<address>(<ADDRESS>@0x1</ADDRESS>)
        }
    }
    """)

    fun `test macros are highlighted`() = checkHighlighting("""
    module 0x1::M {
        fun m() {
            <MACRO>assert!</MACRO>(true, 1);
        }
    }
    """)

    fun `test integer highlighting`() = checkHighlighting("""
    module 0x1::main {
        fun main() {
            let a = <NUMBER>0x123456</NUMBER>;
            
        }
    }
    """)
}
