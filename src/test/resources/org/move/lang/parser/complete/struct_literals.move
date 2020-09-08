module M {
    fun main() {
        let a = Struct { a: val, b: 1 + 1 };
        let a = Struct { a: val, b: Struct2 { val, anotherval: 1 + 1 } };
    }

    fun return_empty_struct() {
        return Struct {};
    }

    fun return_empty_struct_as_expression() {
        Struct {}
    }
}