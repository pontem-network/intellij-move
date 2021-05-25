script {
    use 0x0::Transaction;
    use 0x0::Account;
    // use 0x0::Signer;
    use 0x0::XFI;

    fun main() {}
}

script {
    fun main() {
        let a = 1;
        let b = 2;

        let c = 3;  // comment
    }
}

module MM {
    use 0x0::Transaction;
    use 0x0::Account;
    // use 0x0::Signer;
    use 0x0::XFI;
}

address 0x0 {

module M {}

module M2 {
    use 0x0::Transaction;
    use 0x0::Account;

    fun main() {}

    struct MyStruct {}
}
}

address 0x1 {

module M2 {

    struct T {}
}
}
