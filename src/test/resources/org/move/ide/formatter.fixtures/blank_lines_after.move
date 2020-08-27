script {
    use 0x0::Transaction;
    use 0x0::Account;
    use 0x0::XFI;

    fun main() {}
}

script {
    fun main() {
        let a = 1;
        let b = 2;

        let c = 3;
    }
}

address 0x0 {
    module M {}

    module M2 {
        use 0x0::Transaction;
        use 0x0::Account;
        use 0x0::XFI;

        fun main() {}

        struct MyStruct {}
    }
}