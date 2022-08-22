module M {
    spec from_bytes { // TODO: temporary mockup.
        pragma opaque;
    }

    spec initialize {
        assert true;

        /// After genesis, time progresses monotonically.
        invariant update
            old(is_operating()) ==> old(spec_now_microseconds()) <= spec_now_microseconds();

        /// Conditions we only check for the implementation, but do not pass to the caller.
        aborts_if [concrete]
            (if (proposer == @vm_reserved) {
                now != timestamp
            } else {
                now >= timestamp
            }
            )
            with error::INVALID_ARGUMENT;

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
