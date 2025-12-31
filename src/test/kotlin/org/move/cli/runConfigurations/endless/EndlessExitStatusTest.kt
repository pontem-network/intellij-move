package org.move.cli.runConfigurations.endless

import org.move.utils.tests.MvTestBase

class EndlessExitStatusTest: MvTestBase() {
    fun `test parse result`() {
        val status = EndlessExitStatus.fromJson(
            """
        {
            "Result": "my result message"
        }            
        """
        )
        check(status is EndlessExitStatus.Result)
        check(status.message == "my result message")
    }

    fun `test parse error`() {
        val status = EndlessExitStatus.fromJson(
            """
        {
            "Error": "my error message"
        }            
        """
        )
        check(status is EndlessExitStatus.Error)
        check(status.message == "my error message")
    }

    fun `test parse malformed`() {
        val status = EndlessExitStatus.fromJson(
            """
        {
            "Unknown": "unknown"
        }            
        """
        )
        check(status is EndlessExitStatus.Malformed)
    }

    fun `test parse array of items`() {
        val status = EndlessExitStatus.fromJson(
            """
        {
            "Result": [
                "0000000000000000000000000000000000000000000000000000000000000001::system_addresses",
                "0000000000000000000000000000000000000000000000000000000000000001::guid"
              ]
            }
        }            
        """
        )
        check(status is EndlessExitStatus.Result)
        check(status.message ==
                      "[\"0000000000000000000000000000000000000000000000000000000000000001::system_addresses\"," +
                      "\"0000000000000000000000000000000000000000000000000000000000000001::guid\"]",
              { status.message })
    }
}