script {
    fun main() {
        call();
        call(1, 2);
        call(b"", b"");
        call(b"", x"");
        call(x"", b"");
        call(x"", x"");
        call(1, 2,);
        call(0, 0u8, 0u64, 1u8, 1u64,);
        call(a, a.b, &a, &mut a, copy a, move a);

        call<u8>();
        call<0x1::Mod::S>();
        call<Sender::Mod::S>();
        call<u8,>();
        call<u8, vector<u8>>();
        call<u8, vector<u8>>(Transaction::new<Sender<u8>>());

        Transaction::call<u8, u8, vector<u8>>();

        assert(true, 1);
        assert!(true, 1);
    }
}
