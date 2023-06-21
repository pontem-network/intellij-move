module 0x1::M {
    fun main() {
        let apply = 1;
        let emits = 1;
        let to = 1;
        let except = 1;

        1 ==> 2;
    }
    spec main {
        let post new_a = 1;
        let a = 1..1+1;
        1 ==> 2;

        fun inner_spec_fun() {}
        emits msg to handle if !is_synthetic;
        ensures [concrete] result == 1;
        ensures [global] result == 1;

        apply ModuleInvariant to bar;
        aborts_if exists<Window>(Signer::spec_address_of(to_limit));
        aborts_if exists<Window>(Signer::spec_address_of(to_limit));

        aborts_with [check] 1;
        aborts_with 1, error, Errors::NOT_PUBLISHED;

        ensures exists<CoinType>(@0x1);
        ensures result == TRACE(choose x: u64 where x >= 4 && x <= 5);

        include MySchema;
        include MySchema{ amount };
        include MySchema<MyType>{ amount };
        include MySchema{ address: Signer::address_of(acc) };

        include true ==> MySchema;
        include vote.agree != agree ==> CheckChangeVote<TokenT, ActionT>{vote, proposer_address};

        include if (true) MySchema else MySchema;
        include MySchema && MySchema;

        native fun serialize<MoveValue>(v: &MoveValue): vector<u8>;

        update supply = 1;
    }

    spec module {
        include true ==> MySchema;
    }

    fun main2() {
        1 ==> 2;
    }
}
