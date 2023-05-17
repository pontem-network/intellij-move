package org.move.cli

object AptosWebsiteConnection {
    fun isReachable(): Boolean = isReachableByPing("aptos.dev")

    private fun isReachableByPing(host: String): Boolean {
        return try {
            val pingCommand =
                if (System.getProperty("os.name").startsWith("Windows")) {
                    // For Windows
                    "ping -n 1 $host"
                } else {
                    // For Linux and OSX
                    "ping -c 1 $host"
                }
            val myProcess = Runtime.getRuntime().exec(pingCommand)
            myProcess.waitFor()
            myProcess.exitValue() == 0
        } catch (e: Exception) {
            true
        }
    }
}
