module 0x1::new_lambdas {
    fun main() {
        let a: |u8| u8 with store + drop;
        |a: u8| 1 with store + drop;
    }
}
