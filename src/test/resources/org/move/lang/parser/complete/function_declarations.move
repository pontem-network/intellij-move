module M {
    fun fn(): u8 {}

    fun fn_with_returned_tuple(): (u8, u8) {}

    fun fn_with_type_params<T, U>(): T {}

    fun fn_with_arguments(a: vector<T>, b: u8, c: Transaction::Sender): 0x1::Transaction::Sender {}

    fun fn_with_acquires()
        acquires vector<T>, T, Transaction::Sender {}

    native fun native_function(a: vector<u8>): u8;

    native fun native_function_with_acquires() acquires T;
}