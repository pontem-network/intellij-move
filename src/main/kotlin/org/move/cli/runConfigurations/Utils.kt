package org.move.cli.runConfigurations

import com.intellij.openapi.project.Project
import org.move.cli.moveProjectsService

val Project.hasMoveProject: Boolean get() = moveProjectsService.allProjects.isNotEmpty()