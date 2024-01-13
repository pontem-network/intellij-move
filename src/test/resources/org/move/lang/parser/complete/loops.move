module 0x1::loops {
    fun main() {
        while (true) {
            1;
            break;
            continue;
        };
        loop {
            1;
            break;
            continue;
        };

        let for;
        for (i in 0..10) {
            1;
            break;
            continue;
        };
    }
}
