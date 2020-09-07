module M {
    spec module {
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