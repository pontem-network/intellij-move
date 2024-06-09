module 0x1::access_control {
    fun f1(): u8 acquires S {}

    fun f2(): u8 reads S {}

    fun f3(): u8 writes S {}

    fun f4() acquires S(*) {}

    fun f_multiple() acquires R reads R writes T, S reads G<u64> {}

    fun f5() acquires 0x42::*::* {}

    fun f6() acquires 0x42::m::* {}

    fun f7() acquires *(*) {}

    fun f8() acquires *(0x42) {}

    fun f9(a: address) acquires *(a) {}

    fun f10(param: u64) acquires *(make_up_address(param)) {}

    fun make_up_address(x: u64): address {
    @0x42
    }

    fun f11() !reads *(0x42), *(0x43) {}

    fun f12() pure {}

    fun ok2(s: &signer): bool reads R(signer::address_of(s)) {}

    fun ok2(s: &signer): bool reads R(address_of<u8>(s)) {}
}
