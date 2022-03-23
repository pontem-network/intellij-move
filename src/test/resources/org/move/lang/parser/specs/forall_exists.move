module 0x1::M {
    spec fun spec_call() {
        exists i in 1..100: i == 3
    }
    spec module {
        invariant
            forall addr1: address where old(exists<u8>(addr1)): true;

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

        ensures result ==> (forall j in 0..100: true);
        ensures exists x in 1..10 : x == 1;
    }
}
