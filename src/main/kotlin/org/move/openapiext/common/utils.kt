package org.move.openapiext.common

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.ex.temp.TempFileSystemMarker
import org.move.cli.MvConstants

val isUnitTestMode: Boolean get() = ApplicationManager.getApplication().isUnitTestMode

val VirtualFile.isLightTestFile: Boolean get() = this.fileSystem is TempFileSystemMarker

val VirtualFile.isUnitTestFile: Boolean get() {
//    return isUnitTestMode && isLightTestFile
    if (!isUnitTestMode) return false
//    if (this.name == MvConstants.PSI_FACTORY_DUMMY_FILE) return false
    return this.isLightTestFile
}

fun checkUnitTestMode() = check(isUnitTestMode) { "UnitTestMode needed" }

val isHeadlessEnvironment: Boolean get() = ApplicationManager.getApplication().isHeadlessEnvironment

val isDispatchThread: Boolean get() = ApplicationManager.getApplication().isDispatchThread

val isInternal: Boolean get() = ApplicationManager.getApplication().isInternal
