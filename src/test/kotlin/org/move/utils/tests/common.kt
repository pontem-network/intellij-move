package org.move.utils.tests

import org.jetbrains.annotations.TestOnly
import org.move.cli.MoveProject
import org.move.cli.MoveProjectsService

@TestOnly
fun MoveProjectsService.singleProject(): MoveProject {
    return when (allProjects.size) {
        0 -> error("No cargo projects found")
        1 -> allProjects.single()
        else -> error("Expected single cargo project, found multiple: $allProjects")
    }
}
