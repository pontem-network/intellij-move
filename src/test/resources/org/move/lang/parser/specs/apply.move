module M {
    spec module {
        apply Foo<S> to t;
        apply ExactlyOne to * except initialize;
        apply ModuleInvariant<X, Y> to *foo*<Y, X>;
        apply ModuleInvariant<X, Y> to *foo*<Y, X>, bar except public *, internal baz<X>;
    }
}