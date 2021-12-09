package org.move.cli

import org.move.openapiext.*
import org.toml.lang.psi.TomlKeySegment

fun processNamedAddresses(
    moveProject: MoveProject, addrSubst: Map<String, String>,
    processAddress: (TomlKeySegment, String) -> Boolean
) {
    val currentTableEntries = moveProject.moveToml.tomlFile
        ?.getTable("addresses")
        ?.entries.orEmpty()
    for (tableEntry in currentTableEntries) {
        val segment = tableEntry.singleSegmentOrNull() ?: continue
        val name = segment.name ?: continue
        var value = tableEntry.value?.stringValue() ?: continue
//        var value = tableEntry.?.text?.trim('"') ?: continue
        if (value == "_") {
            value = addrSubst[name] ?: continue
        }
        val toStop = processAddress(segment, value)
        if (toStop) return
    }

    val project = moveProject.project
    for (dep in moveProject.moveToml.dependencies.values) {
        val combinedDepSubst = dep.addrSubst.toMutableMap()
        combinedDepSubst.putAll(addrSubst)
        val depMoveFile =
            dep.absoluteLocalPath
                .resolve(MvConstants.MANIFEST_FILE)
                .findVirtualFile()
                ?.toPsiFile(moveProject.project) ?: continue
        val depMoveProject =
            project.moveProjects.findProjectForPsiElement(depMoveFile) ?: continue
        processNamedAddresses(depMoveProject, combinedDepSubst, processAddress)
    }
}
