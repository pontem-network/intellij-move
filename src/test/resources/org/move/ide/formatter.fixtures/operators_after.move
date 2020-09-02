script {
    fun main() {
        let a = 1;
        let b = 1 + 2 + 3 + 4;
        let c = a == b;

        *b;
        *&b;
        *&*&*&b;

        1 != 0;
    }
}