package org.move.utils

import org.move.stdext.exists
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

object EnvUtils {
    fun findInPATH(binaryName: String): Path? {
        val dirPaths = getEnv("PATH")?.split(":").orEmpty()
        for (dirPath in dirPaths) {
            val path = Paths.get(dirPath, binaryName)
            if (path.exists()) {
                return path
            }
        }
        return null
    }

    fun getEnv(name: String): String? = System.getenv(name)
}

