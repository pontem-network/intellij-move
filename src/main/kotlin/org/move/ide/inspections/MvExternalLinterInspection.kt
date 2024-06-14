/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.move.ide.inspections

import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.codeInspection.*
import com.intellij.codeInspection.ex.GlobalInspectionContextEx
import com.intellij.codeInspection.ex.GlobalInspectionContextUtil
import com.intellij.codeInspection.reference.RefElement
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key
import com.intellij.profile.codeInspection.InspectionProjectProfileManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.util.containers.ContainerUtil
import org.move.cli.MoveProject
import org.move.cli.moveProjectsService
import org.move.cli.runConfigurations.aptos.AptosCompileArgs
import org.move.cli.runConfigurations.aptos.workingDirectory
import org.move.cli.settings.getAptosCli
import org.move.ide.annotator.RsExternalLinterResult
import org.move.ide.annotator.RsExternalLinterUtils
import org.move.ide.annotator.addHighlightsForFile
import org.move.ide.annotator.createDisposableOnAnyPsiChange
import org.move.lang.MoveFile
import org.move.lang.core.psi.ext.ancestorOrSelf
import org.move.lang.moveProject
import org.move.openapiext.rootPluginDisposable

class MvExternalLinterInspection: GlobalSimpleInspectionTool() {

    override fun inspectionStarted(
        manager: InspectionManager,
        globalContext: GlobalInspectionContext,
        problemDescriptionsProcessor: ProblemDescriptionsProcessor
    ) {
        globalContext.putUserData(ANALYZED_FILES, ContainerUtil.newConcurrentSet())
    }

    override fun checkFile(
        file: PsiFile,
        manager: InspectionManager,
        problemsHolder: ProblemsHolder,
        globalContext: GlobalInspectionContext,
        problemDescriptionsProcessor: ProblemDescriptionsProcessor
    ) {
//        if (file !is MoveFile || file.containingCrate.asNotFake?.origin != PackageOrigin.WORKSPACE) return
        if (file !is MoveFile) return
        val analyzedFiles = globalContext.getUserData(ANALYZED_FILES) ?: return
        analyzedFiles.add(file)
    }

    override fun inspectionFinished(
        manager: InspectionManager,
        globalContext: GlobalInspectionContext,
        problemDescriptionsProcessor: ProblemDescriptionsProcessor
    ) {
        if (globalContext !is GlobalInspectionContextEx) return
        val analyzedFiles = globalContext.getUserData(ANALYZED_FILES) ?: return

        val project = manager.project
        val currentProfile = InspectionProjectProfileManager.getInstance(project).currentProfile
        val toolWrapper = currentProfile.getInspectionTool(SHORT_NAME, project) ?: return

        while (true) {
            val anyPsiChangeDisposable = project.messageBus.createDisposableOnAnyPsiChange()
                .also { Disposer.register(project.rootPluginDisposable, it) }
            val moveProjects = run {
                val allProjects = project.moveProjectsService.allProjects
                if (allProjects.size == 1) {
                    setOf(allProjects.first())
                } else {
                    runReadAction {
                        analyzedFiles.mapNotNull { it.moveProject }.toSet()
                    }
                }
            }
            val futures = moveProjects.map {
                ApplicationManager.getApplication().executeOnPooledThread<RsExternalLinterResult?> {
                    checkProjectLazily(it, anyPsiChangeDisposable)?.value
                }
            }
            val annotationResults = futures.mapNotNull { it.get() }

            val exit = runReadAction {
                ProgressManager.checkCanceled()
                if (anyPsiChangeDisposable.isDisposed) return@runReadAction false
                if (annotationResults.size < moveProjects.size) return@runReadAction true
                for (annotationResult in annotationResults) {
                    val problemDescriptors = getProblemDescriptors(analyzedFiles, annotationResult)
                    val presentation = globalContext.getPresentation(toolWrapper)
                    presentation.addProblemDescriptors(problemDescriptors, globalContext)
                }
                true
            }

            if (exit) break
        }
    }

    override fun getDisplayName(): String = "External linter"

    override fun getShortName(): String = SHORT_NAME

    companion object {
        const val SHORT_NAME: String = "MvExternalLinter"

        private val ANALYZED_FILES: Key<MutableSet<MoveFile>> = Key.create("ANALYZED_FILES")

        private fun checkProjectLazily(
            moveProject: MoveProject,
            disposable: Disposable
        ): Lazy<RsExternalLinterResult?>? = runReadAction {
            val project = moveProject.project
            val aptosCli = project.getAptosCli(disposable) ?: return@runReadAction null
            RsExternalLinterUtils.checkLazily(
                aptosCli,
                project,
                moveProject.workingDirectory,
                AptosCompileArgs.forMoveProject(moveProject)
            )
        }

        private fun getProblemDescriptors(
            analyzedFiles: Set<MoveFile>,
            annotationResult: RsExternalLinterResult
        ): List<ProblemDescriptor> = buildList {
            for (file in analyzedFiles) {
                if (!file.isValid) continue
                val highlights = mutableListOf<HighlightInfo>()
                highlights.addHighlightsForFile(file, annotationResult)
                highlights.mapNotNull { ProblemDescriptorUtil.toProblemDescriptor(file, it) }.forEach(::add)
            }
        }

        private fun InspectionToolResultExporter.addProblemDescriptors(
            descriptors: List<ProblemDescriptor>,
            context: GlobalInspectionContext
        ) {
            if (descriptors.isEmpty()) return
            val problems = hashMapOf<RefElement, MutableList<ProblemDescriptor>>()

            for (descriptor in descriptors) {
                val element = descriptor.psiElement ?: continue
                val refElement = getProblemElement(element, context) ?: continue
                val elementProblems = problems.getOrPut(refElement) { mutableListOf() }
                elementProblems.add(descriptor)
            }

            for ((refElement, problemDescriptors) in problems) {
                val descriptions = problemDescriptors.toTypedArray<CommonProblemDescriptor>()
                addProblemElement(refElement, false, *descriptions)
            }
        }

        private fun getProblemElement(element: PsiElement, context: GlobalInspectionContext): RefElement? {
            val problemElement = element.ancestorOrSelf<MoveFile>()
            val refElement = context.refManager.getReference(problemElement)
            return if (refElement == null && problemElement != null) {
                GlobalInspectionContextUtil.retrieveRefElement(element, context)
            } else {
                refElement
            }
        }
    }
}
