module M {
    spec fun initialize {
        assert true;
    }

    spec MyStruct {
        assert true;
    }

    spec schema MySchema {
        assert true;
    }

    spec module {
        fun is_currency(): bool {
            true
        }
    }

    spec fun spec_is_lbr<CoinType>(): bool {
        type<CoinType>() == type<LBR>();
    }
}
