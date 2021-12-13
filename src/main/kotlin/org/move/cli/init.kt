package org.move.cli

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.move.lang.moveProject
import org.move.openapiext.contentRoots
import org.move.openapiext.findVirtualFile
import org.move.openapiext.parseTomlFromFile
import org.move.openapiext.toPsiFile
import org.move.stdext.deepIterateChildrenRecursivery
import org.toml.lang.psi.TomlKeyValue

sealed class InitResult {
    class Ok(val moveProject: MoveProject) : InitResult()
    class Err(val message: String) : InitResult()
}

fun ok(moveProject: MoveProject) = InitResult.Ok(moveProject)
fun error(message: String) = InitResult.Err(message)

fun findMoveTomlFilesDeepestFirst(project: Project): Sequence<VirtualFile> {
//    val contentRoots = project.contentRoots
//    val moveFiles = mutableSetOf<Pair<Int, VirtualFile>>()
//    for (contentRoot in contentRoots) {
//        deepIterateChildrenRecursivery(
//            contentRoot, { it.name == "Move.toml" })
//        {
//            val depth = it.path.split("/").count()
//            moveFiles.add(Pair(depth, it))
//            true
//        }
//    }
    return findMoveTomlFiles(project)
        .sortedByDescending { it.path.split("/").count() }
//    return moveFiles.asSequence().sortedByDescending { it.first }.map { it.second }
}

fun findMoveTomlFiles(project: Project): Sequence<VirtualFile> {
    val contentRoots = project.contentRoots
    val moveFiles = mutableSetOf<VirtualFile>()
    for (contentRoot in contentRoots) {
        deepIterateChildrenRecursivery(
            contentRoot, { it.name == "Move.toml" })
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
        val dependencyAddresses = parseDependencyAddresses(moveToml)
        MoveProject(project, moveToml, projectRoot, dependencyAddresses)
    }
}

private fun parseDependencyAddresses(moveToml: MoveToml): DependencyAddresses {
    val values = mutableAddressMap()
    val placeholders = mutableMapOf<String, TomlKeyValue>()
    for ((addressName, addressVal) in moveToml.addresses.entries) {
        val (value, tomlKeyValue) = addressVal
        if (addressVal.first == MvConstants.ADDR_PLACEHOLDER) {
            placeholders[addressName] = tomlKeyValue
        } else {
            values[addressName] = AddressVal(value, tomlKeyValue, null)
        }
    }
    return DependencyAddresses(values, placeholders)
}
