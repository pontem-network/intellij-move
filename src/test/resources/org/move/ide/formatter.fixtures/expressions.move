module 0x1::expressions {
    fun main(): bool {
        if (true
                && false
        ) {};

        if (
        true
        && false
        )
        true
        else
        false;

        vector[
        1,
        1,
        ];

        while (true)
        1 + 1;

        return 1 == 2
        && (1 == 3
        && 1 == 4);
    }
}
