package org.move.ide.newProject

import com.intellij.ide.util.PsiNavigationSupport
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.move.cli.runConfigurations.aptos.AptosCli.Companion.GeneratedFilesHolder
import org.move.openapiext.common.isHeadlessEnvironment

fun Project.openFile(file: VirtualFile) = openFiles(GeneratedFilesHolder(file))

fun Project.openFiles(files: GeneratedFilesHolder) = invokeLater {
    if (!isHeadlessEnvironment) {
        val navigation = PsiNavigationSupport.getInstance()
        navigation.createNavigatable(this, files.manifest, -1).navigate(false)
    }
}
