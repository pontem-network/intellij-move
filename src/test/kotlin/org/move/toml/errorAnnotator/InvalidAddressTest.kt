package org.move.toml.errorAnnotator

class InvalidAddressTest: MoveTomlAnnotatorTestBase() {
    fun `test valid addresses`() = checkMoveTomlWarnings(
        """
        [addresses]
        addr0 = "_"
        addr1 = "0x1"
        addr2 = "0x42"
        addr3 = "0x4242424242424242424242424242424242424242424242424242420000000000"
        addr4 = "4242424242424242424242424242424242424242424242424242420000000000"
    """
    )

    fun `test invalid symbols in address`() = checkMoveTomlWarnings(
        """
        [addresses]
        addr2 = "0x<error descr="Invalid address: only hex symbols are allowed">helloworld</error>"
    """
    )

    fun `test address is too long`() = checkMoveTomlWarnings(
        """
        [addresses]
        addr3 = "0x<error descr="Invalid address: no more than 64 symbols allowed">424242424242424242424242424242424242424242424242424242000000000011122</error>"
    """
    )
}