package org.move.utils.tests.resolve

import com.intellij.openapi.vfs.VirtualFile

fun checkResolvedFile(actualResolveFile: VirtualFile, expectedFilePath: String, pathResolver: (String) -> VirtualFile?): TestResolveResult {
    if (expectedFilePath.startsWith("...")) {
        if (!actualResolveFile.path.endsWith(expectedFilePath.drop(3))) {
            return TestResolveResult.Err("Should resolve to $expectedFilePath, was ${actualResolveFile.path} instead")
        }
    } else {
        val expectedResolveFile = pathResolver(expectedFilePath)
            ?: return TestResolveResult.Err("Can't find `$expectedFilePath` file")

        if (actualResolveFile != expectedResolveFile) {
            return TestResolveResult.Err("Should resolve to ${expectedResolveFile.path}, was ${actualResolveFile.path} instead")
        }
    }
    return TestResolveResult.Ok
}

sealed class TestResolveResult {
    object Ok : TestResolveResult()
    data class Err(val message: String) : TestResolveResult()
}
