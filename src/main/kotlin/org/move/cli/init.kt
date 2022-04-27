package org.move.cli

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.move.openapiext.contentRoots
import org.move.openapiext.parseTomlFromFile
import org.move.stdext.deepIterateChildrenRecursivery

sealed class InitResult {
    class Ok(val moveProject: MoveProject) : InitResult()
    class Err(val message: String) : InitResult()
}

fun ok(moveProject: MoveProject) = InitResult.Ok(moveProject)
fun error(message: String) = InitResult.Err(message)

fun findMoveTomlFilesDeepestFirst(project: Project): Sequence<VirtualFile> {
    return findMoveTomlFiles(project)
        .sortedByDescending { it.path.split("/").count() }
}

fun findMoveTomlFiles(project: Project): Sequence<VirtualFile> {
    val contentRoots = project.contentRoots
    val moveFiles = mutableSetOf<VirtualFile>()
    for (contentRoot in contentRoots) {
        deepIterateChildrenRecursivery(
            contentRoot, { it.name == MvConstants.MANIFEST_FILE })
        {
            moveFiles.add(it)
            true
        }
    }
    return moveFiles.asSequence()
}

fun initializeMoveProject(project: Project, fsMoveTomlFile: VirtualFile): MoveProject? {
    return runReadAction {
        val tomlFile = parseTomlFromFile(project, fsMoveTomlFile) ?: return@runReadAction null
        val projectRoot = fsMoveTomlFile.parent!!
        val moveToml = MoveToml.fromTomlFile(tomlFile, projectRoot.toNioPath())
        val addresses = parseAddresses(moveToml.addresses, moveToml.packageName ?: "")
        val devAddresses = parseAddresses(moveToml.dev_addresses, moveToml.packageName ?: "")
        MoveProject(project, moveToml, projectRoot, addresses, devAddresses)
    }
}

private fun parseAddresses(rawAddressMap: RawAddressMap, packageName: String): DeclaredAddresses {
    val values = mutableAddressMap()
    val placeholders = placeholderMap()
    for ((addressName, addressVal) in rawAddressMap.entries) {
        val (value, tomlKeyValue) = addressVal
        if (addressVal.first == MvConstants.ADDR_PLACEHOLDER) {
            placeholders[addressName] = PlaceholderVal(tomlKeyValue, packageName)
        } else {
            values[addressName] = AddressVal(value, tomlKeyValue, null, packageName)
        }
    }
    return DeclaredAddresses(values, placeholders)
}
