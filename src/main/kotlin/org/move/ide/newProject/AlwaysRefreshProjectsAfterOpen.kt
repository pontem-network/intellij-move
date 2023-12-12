package org.move.ide.newProject

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import org.move.cli.moveProjectsService
import org.move.openapiext.common.isUnitTestMode
import org.move.openapiext.debugInProduction

class AlwaysRefreshProjectsAfterOpen: ProjectActivity {
    override suspend fun execute(project: Project) {
        LOG.debugInProduction("activity started")
        if (!isUnitTestMode) {
            project.moveProjectsService.scheduleProjectsRefresh("IDE project opened")
        } else {
            LOG.warn("Skip REFRESH [IDE project opened] in unit tests")
        }
    }

    companion object {
        private val LOG = logger<AlwaysRefreshProjectsAfterOpen>()
    }
}