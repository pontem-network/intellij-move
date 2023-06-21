module 0x1::m {
    spec module {
        local two: u8;
        local two_with_params: u8;
        global three: u8;
        global four<X, Y>: u8 = 1;
    }
}
