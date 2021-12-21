module M {
    /// this is docstring
    /// with two lines
    struct MyStruct<T> {
        addr: <BUILTIN_TYPE>address</BUILTIN_TYPE>,
        val: <PRIMITIVE_TYPE>bool</PRIMITIVE_TYPE>
        field: <TYPE_PARAMETER>T</TYPE_PARAMETER>
    }

    fun main(signer: &<BUILTIN_TYPE>signer</BUILTIN_TYPE>, val: <PRIMITIVE_TYPE>u8</PRIMITIVE_TYPE>, s: MyStruct) {
        // this is comment
        let local_val = val;

        let myresource = <BUILTIN_FUNCTION_CALL>move_from</BUILTIN_FUNCTION_CALL>(signer);
        <MACRO>assert!</MACRO>(true, 1);
    }
}
