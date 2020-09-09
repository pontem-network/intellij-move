module M {
    struct MyStruct<T> {
        addr: <BUILTIN_TYPE>address</BUILTIN_TYPE>,
        val: <PRIMITIVE_TYPE>bool</PRIMITIVE_TYPE>
        field: <TYPE_PARAMETER>T</TYPE_PARAMETER>
    }

    fun main(signer: &<BUILTIN_TYPE>signer</BUILTIN_TYPE>, val: <PRIMITIVE_TYPE>u8</PRIMITIVE_TYPE>, s: MyStruct) {
        let local_val = val;

        let myresource = <BUILTIN_FUNCTION>move_from</BUILTIN_FUNCTION>(signer)
    }
}