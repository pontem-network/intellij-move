package org.move.cli

import com.intellij.execution.filters.Filter
import com.intellij.execution.filters.OpenFileHyperlinkInfo
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import java.nio.file.Path
import java.nio.file.Paths

class MoveFileHyperlinkFilter(
    private val project: Project,
    private val moveProjectBasePath: Path,
) : Filter, DumbAware {

    companion object {
        val FILE_POSITION_RE =
            Regex("""(?<file>(?:\p{Alpha}:)?[0-9 a-z_A-Z\-\\./]+):(?<line>[0-9]+):(?<column>[0-9]+)""")
    }

    override fun applyFilter(line: String, entireLength: Int): Filter.Result? {
        val match = FILE_POSITION_RE.find(line) ?: return null

        val fileGroup = match.groups[1]!!
        val lineNumber = match.groups[2]!!
        val columnNumber = match.groups[3]!!

        val lineStart = entireLength - line.length
        val startInd = lineStart + fileGroup.range.first + 1
        val endInd = lineStart + columnNumber.range.last + 1

        val file = resolveFilePath(fileGroup.value) ?: return null
        val row = lineNumber.value.toInt() - 1
        val column = columnNumber.value.toInt() - 1

        val linkInfo = OpenFileHyperlinkInfo(project, file, row, column)
        return Filter.Result(startInd, endInd, linkInfo)
    }

    private fun resolveFilePath(fileName: String): VirtualFile? {
        val portablePath = FileUtil.toSystemIndependentName(fileName).trim()
        val canonicalPath = Paths.get(moveProjectBasePath
                                          .resolve(portablePath)
                                          .toFile()
                                          .canonicalPath)
        val file = VirtualFileManager.getInstance().findFileByNioPath(canonicalPath)
        return file
    }
}