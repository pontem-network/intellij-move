module 0x1::loop_invariants {
    fun main() {
        while (true) {} spec { assert true };
        while (true) 1 + 1 spec { assert true };
        // loop invariant is written in a spec block inside the loop condition
        while ({spec {assert x < 42;}; n < 64}) {
            spec {
                assert x > 42;
                assert 0 < x;
            };
            n = n + 1
        };

        // the following should parse successfully but fail typing
        spec {} + 1;
        spec {} && spec {};
        &mut spec {};
        (spec {}: ());

        for (i in 1..10 spec { assert true }) {};
        for (i in 0..1 spec {invariant y > 0;}) {
            y = y + 1;
        };
    }
}
