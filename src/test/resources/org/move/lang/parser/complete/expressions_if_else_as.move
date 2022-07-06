script {
    fun main() {
        if (true) *!1 else *!2;
        (if (true) 1 else 2) as u8;
        (2) as u8;
        (a = 2) as u8;
    }
}
