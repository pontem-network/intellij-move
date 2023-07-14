package org.move.cli.sentryReporter

import io.sentry.Sentry
import org.move.cli.MoveProject
import org.move.cli.MoveProjectsService
import org.move.cli.MoveProjectsService.MoveProjectsListener
import org.move.cli.manifest.TomlDependency
import org.move.openapiext.getTable
import org.move.openapiext.getTablesByFirstSegment
import org.move.openapiext.syntheticLibraries

@Suppress("PropertyName")
private data class MoveTomlContext(
    val name: String,
    val dependencies_parsed: List<TomlDependency>,
    val dependencies_raw: List<String>,
)

private data class SyntheticLibraryContext(val roots: List<String>)

private data class MoveProjectContext(
    val moveToml: MoveTomlContext,
    val syntheticLibraries: List<SyntheticLibraryContext>
)

class SentryContextsProjectsListener : MoveProjectsListener {
    override fun moveProjectsUpdated(service: MoveProjectsService, projects: Collection<MoveProject>) {
        Sentry.configureScope {
            it.setContexts(
                "projects",
                projects.mapNotNull { moveProject -> getMoveProjectContext(moveProject) }
            )
        }
    }

    private fun getMoveProjectContext(moveProject: MoveProject): MoveProjectContext? {
        val tomlFile = moveProject.currentPackage.moveToml.tomlFile ?: return null

        val rawDeps = mutableListOf<String>()
        val depsTable = tomlFile.getTable("dependencies")
        if (depsTable != null) {
            rawDeps.add(depsTable.text)
        }
        for (depInlineTable in tomlFile.getTablesByFirstSegment("dependencies")) {
            rawDeps.add(depInlineTable.text)
        }

        val moveTomt = MoveTomlContext(
            name = moveProject.currentPackage.packageName,
            dependencies_raw = rawDeps,
            dependencies_parsed = moveProject.currentPackage.moveToml.deps.map { it.first },
        )
        val syntheticLibraries =
            moveProject.project.syntheticLibraries
                .map { SyntheticLibraryContext(it.allRoots.map { it.path }) }
        return MoveProjectContext(moveTomt, syntheticLibraries)
    }
}