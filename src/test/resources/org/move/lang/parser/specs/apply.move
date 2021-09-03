module M {
    spec module {
        apply Foo<S> to *foo;
        apply ExactlyOne to * except initialize;
        apply ModuleInvariant<X, Y> to *foo*<Y, X>;
        apply EnsuresHasKeyRotationCap { account: new_account } to make_account;
        apply ModuleInvariant<X, Y> to *foo*<Y, X>, bar except public *, internal baz<X>;
    }
}
