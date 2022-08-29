module 0x1::vectors {
    use std::vector;

    fun main(a: vector<u8>) {
        vector[];
        vector[1, 1];
        vector[
            type_info,
            type_info,
        ];
        vector<u8>[1, 2, 3];
        vector::add();
    }
}
