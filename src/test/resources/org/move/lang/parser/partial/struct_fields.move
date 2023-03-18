module M {
    struct T {
        : u8,
        my_field: u8
    }
    struct T {
        my_field: ,
        my_field: u8
    }
    struct T {
        my_field: u8
        my_field: u8
    }

    struct NoTypesFields {
        field1,
        field2:,
        field3
    }

    fun main() {
        let T { my:  } = 1;
        let T { my: , my_field } = 1;
        let T { my: , my_field: , my_field2: _ } = 1;
        let T { my myfield } = 1;
        let T { my myfield: _ } = 1;
    }
}

module M {
    fun main() {
        T { my:  };
        T { my: , my_field: , my_field2: 2 };
        T { my myfield: 1 };
        T { my myfield };
        T { my myfield: _ };
    }
}
