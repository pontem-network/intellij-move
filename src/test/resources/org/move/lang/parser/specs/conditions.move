module 0x1::M {
    spec module {
        sending: &mut signer;

        amount: u64;
        local amount: u64;
        global amount: u64;

        invariant x > 0;
        invariant<T> x > 0;
        axiom x > 0;
        axiom<T> x > 0;
        invariant x == y;

        invariant global<Counter>(a).value < 128;
        invariant update old(y) < x;
        invariant update expected_coin_sum = expected_coin_sum - old(x) + x;
        invariant pack expected_coin_sum = expected_coin_sum + x;
        invariant unpack expected_coin_sum = expected_coin_sum - x;

        invariant [global] x > 0;
        invariant [global, isolated] x > 0;
        invariant [global, isolated, deactivated] x > 0;
        invariant update [global] x > 0;

        aborts_if x == 0;
        aborts_if y == 0;
        aborts_if 0 == y
            with Error::MY_ERROR;

        ensures RET == x + 1;
        ensures RET == x/y;
        ensures x/y == RET;
        ensures RET == {let y = x; y + 1};
        ensures all(x, |y, z| x + y + z);
        ensures RET == x[1] && x[0..3];
        ensures x > 0 ==> RET == x - 1;
        ensures x == 0 ==> RET == x;

        ensures generic<T> == 1;
        ensures Self::generic<T> == 1;

        choose a: address where exists<R>(a) && global<R>(a).value > 0;
        choose min i: num where in_range(v, i) && v[i] == 2;
}

    spec schema ModuleInvariant<X, Y> {
        requires global<X>(0x0).f == global<X>(0x1).f;
        ensures global<X>(0x0).f == global<X>(0x1).f;
    }
}
