module M {
    spec module {
        invariant x > 0;
        invariant x == y;

        invariant update old(y) < x;
        invariant update expected_coin_sum = expected_coin_sum - old(x) + x;
        invariant pack expected_coin_sum = expected_coin_sum + x;
        invariant unpack expected_coin_sum = expected_coin_sum - x;

        invariant forall x: num, y: num, z: num : x == y && y == z ==> x == z;
        invariant forall x: num : exists y: num : y >= x;
        invariant exists x in 1..10, y in 8..12 : x == y;

        aborts_if x == 0;
        aborts_if y == 0;
        aborts_if 0 == y;

        ensures RET == x + 1;
        ensures RET == x/y;
        ensures x/y == RET;
        ensures RET = {let y = x; y + 1};
        ensures all(x, |y, z| x + y + z);
        ensures RET = x[1] && x[0..3];
        ensures x > 0 ==> RET == x - 1;
        ensures x == 0 ==> RET == x;

        ensures generic<T> == 1;
        ensures Self::generic<T> == 1;
    }

    spec schema ModuleInvariant<X, Y> {
        requires global<X>(0x0).f == global<X>(0x1).f;
        ensures global<X>(0x0).f == global<X>(0x1).f;
    }
}