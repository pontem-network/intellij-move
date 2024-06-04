module 0x1::<MODULE>main</MODULE> {
    const <CONSTANT>MAX_INT</CONSTANT>: <PRIMITIVE_TYPE>u8</PRIMITIVE_TYPE> = 255;

    /// this is docstring
    /// with two lines
    struct MyStruct<T> {
        addr: <BUILTIN_TYPE>address</BUILTIN_TYPE>,
        val: <PRIMITIVE_TYPE>bool</PRIMITIVE_TYPE>
        field: <TYPE_PARAMETER>T</TYPE_PARAMETER>
    }

    fun <FUNCTION>main</FUNCTION>(signer: &<BUILTIN_TYPE>signer</BUILTIN_TYPE>, val: <PRIMITIVE_TYPE>u8</PRIMITIVE_TYPE>, s: MyStruct) {
        // this is comment
        let local_val = val;

        <FUNCTION_CALL>call</FUNCTION_CALL>(1);
        let myresource = <BUILTIN_FUNCTION_CALL>move_from</BUILTIN_FUNCTION_CALL>(signer);
        <MACRO>assert!</MACRO>(true, 1);

        <VECTOR_LITERAL>vector</VECTOR_LITERAL>[];
    }

    entry fun <ENTRY_FUNCTION>entry_fun</ENTRY_FUNCTION>() {}
    inline fun <INLINE_FUNCTION>inline_fun</INLINE_FUNCTION>() {}
    #[view]
    public fun <VIEW_FUNCTION>view_fun</VIEW_FUNCTION>() {}

    fun <METHOD>receiver</METHOD>(<SELF_PARAMETER>self</SELF_PARAMETER>: S, <VARIABLE>self</VARIABLE>: u8): u8 {
        <SELF_PARAMETER>self</SELF_PARAMETER>.field
    }
    fun main(s: S) {
        s.<METHOD_CALL>receiver</METHOD_CALL>();
    }
}
