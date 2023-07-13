module 0x1::spec_properties {
    spec module {
        invariant [] true;
        invariant [seed = 1] true;
        invariant [abstract, concrete, verify = true] 1 == 1;
        invariant [seed] true
    }
}
