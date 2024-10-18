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

    inline fun fold<Element>(a: (Element),
                             f: (|Element| Element),
                             g: |Element, (|Element| Element)| Element): Element {
        f(a, g)
    }

    fun main(f: ||, g: ||u64) {
        foo(|| {});
        bar(|| 1);

    }
}
