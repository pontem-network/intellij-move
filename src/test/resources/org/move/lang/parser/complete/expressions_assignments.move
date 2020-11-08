script {
    fun main() {
        let a = 1 + 1;
        let b = (a = 1);

        *&a = 1;
        &a.b = 1;
        *a = *1 + *1;
        *a = *1 * *1;
        *a = *!1 + *!2;
    }
}