module M {
    struct MyStruct {
        addr: <BUILTIN_TYPE>address</BUILTIN_TYPE>,
        val: <PRIMITIVE_TYPE>bool</PRIMITIVE_TYPE>
    }

    fun main(signer: &<BUILTIN_TYPE>signer</BUILTIN_TYPE>, val: <PRIMITIVE_TYPE>u8</PRIMITIVE_TYPE>, s: MyStruct) {
        let local_val = val;

        let myresource = <BUILTIN_FUNCTION>move_from</BUILTIN_FUNCTION>(signer)
    }
}