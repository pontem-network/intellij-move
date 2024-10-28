module 0x1::loop_labels {
    fun example(x: u64): u64 {
        'label1: while (x > 10) {
            loop {
                if (x % 2 == 0) {
                    continue 'label1;
                } else {
                    break 'label1
                }
            }
        };
        x
    }
}