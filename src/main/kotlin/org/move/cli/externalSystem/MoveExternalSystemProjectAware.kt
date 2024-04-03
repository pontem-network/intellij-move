package org.move.cli.projectAware

//class MoveExternalSystemProjectAware(
//    private val project: Project
//) : ExternalSystemProjectAware {
//
//    override val projectId: ExternalSystemProjectId = ExternalSystemProjectId(MOVE_SYSTEM_ID, project.name)
//
//    override val settingsFiles: Set<String>
//        get() {
//            val settingsFilesService = MoveSettingsFilesService.getInstance(project)
//            // Always collect fresh settings files
//            return settingsFilesService.collectSettingsFiles().toSet()
//        }
//
//    override fun reloadProject(context: ExternalSystemProjectReloadContext) {
//        FileDocumentManager.getInstance().saveAllDocuments()
//        project.moveProjects.refreshAllProjects()
//    }
//
//    override fun subscribe(listener: ExternalSystemProjectListener, parentDisposable: Disposable) {
//        project.messageBus.connect(parentDisposable).subscribe(
//            MoveProjectsService.MOVE_PROJECTS_REFRESH_TOPIC,
//            object : MoveProjectsService.MoveProjectsRefreshListener {
//                override fun onRefreshStarted() {
//                    listener.onProjectReloadStart()
//                }
//
//                override fun onRefreshFinished(status: MoveProjectsService.MoveRefreshStatus) {
//                    val externalStatus = when (status) {
//                        MoveProjectsService.MoveRefreshStatus.SUCCESS -> ExternalSystemRefreshStatus.SUCCESS
//                        MoveProjectsService.MoveRefreshStatus.FAILURE -> ExternalSystemRefreshStatus.FAILURE
//                        MoveProjectsService.MoveRefreshStatus.CANCEL -> ExternalSystemRefreshStatus.CANCEL
//                    }
//                    listener.onProjectReloadFinish(externalStatus)
//                }
//            }
//        )
//    }
//
//    companion object {
//        val MOVE_SYSTEM_ID: ProjectSystemId = ProjectSystemId("Move")
//
//        fun register(project: Project, disposable: Disposable) {
//            val moveProjectAware = MoveExternalSystemProjectAware(project)
//            val projectTracker = ExternalSystemProjectTracker.getInstance(project)
//            projectTracker.register(moveProjectAware, disposable)
//            projectTracker.activate(moveProjectAware.projectId)
//
//            project.messageBus.connect(disposable)
//                .subscribe(MoveProjectSettingsService.MOVE_SETTINGS_TOPIC,
//                           object : MoveSettingsListener {
//                               override fun moveSettingsChanged(e: MoveSettingsChangedEvent) {
//                                   AutoImportProjectTracker.getInstance(project)
//                                       .markDirty(moveProjectAware.projectId)
//                               }
//                           })
//        }
//    }
//}
