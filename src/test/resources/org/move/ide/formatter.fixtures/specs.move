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
define is_currency(): bool {
    true
}
}

spec define spec_is_lbr<CoinType>(): bool {
type<CoinType>() == type<LBR>();
}
}
