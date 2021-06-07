module M {
    spec module {
        invariant
            forall addr1: address
            where old(exists(addr1));

        invariant
            forall x: num, y: num, z: num
                : x == y && y == z ==> x == z;
        invariant
            forall x: num
            : exists y: num
                : y >= x;

        invariant
            forall x: num where true:
                forall y: num where false:
                    x >= y;

        invariant exists x in 1..10, y in 8..12 : x == y;
        invariant exists x in 1..10, y in 8..12 : x == y;
    }
}
