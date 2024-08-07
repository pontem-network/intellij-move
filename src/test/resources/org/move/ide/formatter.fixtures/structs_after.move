module M {
    struct MyStruct {
        a: u8,
        b: u8,
        c: u8,
    }
}

module M2 {
    struct MyStruct {
        a: u8,
        b: u8,
        c: u8,
    }

    fun main() {
        call(MyStruct {
            a: val, b: anotherval
        });

        call(
            a,
            MyStruct {
                a: val, b: anotherval
            }
        );

        let a = MyStruct { val };
        let a = MyStruct { val: myval };
        let a = MyStruct<u8> { val: myval };

        let MyStruct { val } = get_struct();
        let MyStruct { val: myval } = get_struct();
    }
}
