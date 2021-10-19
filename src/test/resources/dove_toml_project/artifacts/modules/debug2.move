address 0x1 {

module Debug2 {
    native public fun print<T>(x: &T);
    native public fun print_stack_trace();
}
}
