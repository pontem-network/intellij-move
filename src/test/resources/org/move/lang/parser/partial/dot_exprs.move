module 0x1::dot_exprs {
    fun m() {
        bin. assert!(true, 1);

        bin. S { field: 1 };

        bin. vector[];

        bin.field bin.field

        bin. aptos_token::get_token();

        bin. b"
        bin. x"
        bin. return
    }

    fun receiver() {
        bin.no_type_args_without_colon_colon<u8>;
    }
}
