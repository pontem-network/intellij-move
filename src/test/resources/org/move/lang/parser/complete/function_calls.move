script {
    fun main() {
        call();
        call(1, 2);
        call(1, 2,);

        call<u8>();
        call<u8,>();
        call<u8, vector<u8>>();

        Transaction::call<u8, u8, vector<u8>>();
    }
}