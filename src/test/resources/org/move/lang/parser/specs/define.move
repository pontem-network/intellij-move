module M {
    spec module {
        define define1(a: u8): bool {
            requires a > 0
        }

        define define2(a: u8): bool {
            ensures a > 0
        }

        define define3(a: u8): bool {
            invariant a > 0
        }

        define atLeastOne(): bool {
            exists a: address: exists<R>(a)
        }

        define atMostOne(): bool {
            forall a: address, b: address where exists<R>(a) & & exists<R>(b): a == b
        }

        define exists_R(addr: address): bool {
            exists<R>(addr)
        }
    }

    spec define exists_R(addr: address): bool {
        let e = exists<R>(addr);
        e && e || e
    }

}