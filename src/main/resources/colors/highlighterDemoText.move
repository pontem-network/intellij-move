module M {
    struct MyStruct {
        addr: <PRIMITIVE_TYPE>address</PRIMITIVE_TYPE>
    }

    fun main(signer: &<PRIMITIVE_TYPE>signer</PRIMITIVE_TYPE>, val: <PRIMITIVE_TYPE>u8</PRIMITIVE_TYPE>, s: MyStruct) {
        let local_val = val;

        let myresource = <BUILTIN_FUNCTION>move_from</BUILTIN_FUNCTION>(signer)
    }
}