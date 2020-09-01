module M {
    fun main() {
        (1 + 1) * (1 + 1);
        (!!true + !!true) * !!false;
        1 % 2;
        1 ^ 2;

        let a = 1 + 1;
        let b = let a = 1;
        let b = (let a = 1);

        *&a = 1;
        &a.b = 1;
        *a = *1 + *1;
        *a = *1 * *1;
        *a = if (true) *!1 else *!2;
        *a = 1 + 2 * (3 + 5) * 4;

        a = 1 as u8;
        (if (true) 1 else 2) as u8;
        (2) as u8;
        (let a = 1) as u8;
        (a = 2) as u8;
    }

    spec fun main {
        a ==> b;
        a <==> b;
        x && y ==> x;
        x && y <==> x;
    }
}