address named = 0x1;
address alice = {{alice}};

/* my module */
module 0x1::M {
    fun main() {
        @0;
        @01;
        @0x0;
        @0x0000;
        @0x00001111;
        @wallet1pxqfjvnu0utauj8fctw2s7j4mfyvrsjd59c2u8;
        @5FHneW46xGXgs5mUiveU4sbTyGBzmstUspZC92UhjJM694ty;
        @Std;
        @{{alice}};
        @DiemFramework;

        @0x89b9f9d1fadc027cf9532d6f9904152222331122;
        @0x89b9f9d1fadc027cf9532d6f9904152289b9f9d1fadc027cf9532d6f99041522;
    }
}

address 0x111 {
module M {}
}

address wallet1pxqfjvnu0utauj8fctw2s7j4mfyvrsjd59c2u8 {
module M {}
}

address 5FHneW46xGXgs5mUiveU4sbTyGBzmstUspZC92UhjJM694ty {
module M {}
}

address DiemFramework {
module M1 {

}
}

module 0x111::M2 {}
module DiemFramework::M2 {}
