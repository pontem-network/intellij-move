module M {
    fun main(    ) {}
    fun main2     (a:u8, b : u8,c :u8     ) :u8 {}
    fun main3 < T , U > () {}
    fun main_signer(mysigner: &signer) {}
    fun main_signer2(mysigner: &mut signer) {}

    public fun public_main()acquires S {}

    public ( script )  fun public_script_main() {}

    public ( friend )  fun public_friend_main() {}
}
