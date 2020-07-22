script {
    fun main() {
        while (v < 10) v = v + 1;
        while (v < 10) { v = v + 1 };
        while (v < 10) ( v = v + 1 );
        while (v < 10) ( v: u8 );
        while (v < 10) ( (v = v + 1): u8 );
    }
}