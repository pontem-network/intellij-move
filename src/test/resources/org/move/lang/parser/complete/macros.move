module 0x1::macros {
    public inline fun map<Element, NewElement>(
        v: vector<Element>,
        f: |Element|NewElement,
    ): vector<NewElement> {
        foreach(v, |elem| push_back(&mut result, f(elem)));
    }

    inline fun filter<Element: drop>(
        p: |&Element|
    ) {
        foreach(v, |elem| {
            if (p(&elem)) push_back(&mut result, elem);
        });
    }
}
