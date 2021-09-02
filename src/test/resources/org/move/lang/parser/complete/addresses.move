module M {
    fun main() {
        @0x0;
        @0x0000;
        @0x00001111;
        @wallet1pxqfjvnu0utauj8fctw2s7j4mfyvrsjd59c2u8;
        @5FHneW46xGXgs5mUiveU4sbTyGBzmstUspZC92UhjJM694ty;
        {{sender}};
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

address {{sender}} {
module M {

}
}

address DiemFramework {
module M {

}
}

module 0x111::M {}
module DiemFramework::M {}
