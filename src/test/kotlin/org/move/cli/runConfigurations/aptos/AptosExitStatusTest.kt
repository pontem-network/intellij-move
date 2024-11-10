package org.move.cli.runConfigurations.aptos

import org.move.utils.tests.MvTestBase

class AptosExitStatusTest: MvTestBase() {
    fun `test parse result`() {
        val status = AptosExitStatus.fromJson(
            """
        {
            "Result": "my result message"
        }            
        """
        )
        check(status is AptosExitStatus.Result)
        check(status.message == "my result message")
    }

    fun `test parse error`() {
        val status = AptosExitStatus.fromJson(
            """
        {
            "Error": "my error message"
        }            
        """
        )
        check(status is AptosExitStatus.Error)
        check(status.message == "my error message")
    }

    fun `test parse malformed`() {
        val status = AptosExitStatus.fromJson(
            """
        {
            "Unknown": "unknown"
        }            
        """
        )
        check(status is AptosExitStatus.Malformed)
    }
}