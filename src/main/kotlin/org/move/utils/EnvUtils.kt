package org.move.utils

import com.intellij.util.EnvironmentUtil
import org.move.stdext.exists
import java.nio.file.Path
import java.nio.file.Paths

object EnvUtils {
    fun findInPATH(binaryName: String): Path? {
        val dirPaths = EnvironmentUtil.getValue("PATH")?.split(":").orEmpty()
        for (dirPath in dirPaths) {
            val path = Paths.get(dirPath, binaryName)
            if (path.exists()) {
                return path
            }
        }
        return null
    }
}

