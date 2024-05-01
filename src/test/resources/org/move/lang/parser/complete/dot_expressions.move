module 0x1::dot_expressions {
    fun dot() {
        bin.field1.field2;
        bin.field < 1;
        bin.receiver_func();
        bin.field.call();

        bin.call::<T>();
        bin.call::<T, U>();
        vector<u8>[1].length::<u8>();
    }
    spec dot {
        bin.field[1];
    }
}
