module 0x1::<MODULE>main</MODULE> {
    const <CONSTANT>MAX_INT</CONSTANT>: <PRIMITIVE_TYPE>u8</PRIMITIVE_TYPE> = 255;

    /// this is docstring
    /// with two lines
    struct <STRUCT>MyStruct</STRUCT><<TYPE_PARAMETER>T</TYPE_PARAMETER>> {
        <FIELD>addr</FIELD>: <BUILTIN_TYPE>address</BUILTIN_TYPE>,
        <FIELD>val</FIELD>: <PRIMITIVE_TYPE>bool</PRIMITIVE_TYPE>
        <FIELD>field</FIELD>: <TYPE_PARAMETER>T</TYPE_PARAMETER>
    }

    <KEYWORD>enum</KEYWORD> <ENUM>Thing</ENUM> {
        <ENUM_VARIANT>Plain</ENUM_VARIANT>
        <ENUM_VARIANT>Positional</ENUM_VARIANT>(<PRIMITIVE_TYPE>u8</PRIMITIVE_TYPE>)
        <ENUM_VARIANT>Structural</ENUM_VARIANT> { <FIELD>x</FIELD>: <PRIMITIVE_TYPE>u8</PRIMITIVE_TYPE>, <FIELD>y</FIELD>: <PRIMITIVE_TYPE>u8</PRIMITIVE_TYPE> }
    }

    fun <FUNCTION>main</FUNCTION>(signer: &<BUILTIN_TYPE>signer</BUILTIN_TYPE>, val: <PRIMITIVE_TYPE>u8</PRIMITIVE_TYPE>, s: <STRUCT>MyStruct</STRUCT>) {
        // this is comment
        let <VARIABLE>local_val</VARIABLE> = val;

        <FUNCTION_CALL>call</FUNCTION_CALL>(1);
        let <VARIABLE>myresource</VARIABLE> = <BUILTIN_FUNCTION_CALL>move_from</BUILTIN_FUNCTION_CALL>(signer);
        <MACRO>assert!</MACRO>(true, 1);

        <VECTOR_LITERAL>vector</VECTOR_LITERAL>[];
    }

    entry fun <ENTRY_FUNCTION>entry_fun</ENTRY_FUNCTION>() {}
    inline fun <INLINE_FUNCTION>inline_fun</INLINE_FUNCTION>() {}
    <ATTRIBUTE>#[view]</ATTRIBUTE>
    public fun <VIEW_FUNCTION>view_fun</VIEW_FUNCTION>() {}

    fun <METHOD>receiver</METHOD>(<SELF_PARAMETER>self</SELF_PARAMETER>: S, <VARIABLE>self</VARIABLE>: <PRIMITIVE_TYPE>u8</PRIMITIVE_TYPE>): <PRIMITIVE_TYPE>u8</PRIMITIVE_TYPE> {
        <SELF_PARAMETER>self</SELF_PARAMETER>.<FIELD>field</FIELD>
    }
    fun <FUNCTION>main</FUNCTION>(s: S) {
        s.<METHOD_CALL>receiver</METHOD_CALL>();
    }
}
