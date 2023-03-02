/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.move.lang.core.psi

import com.intellij.ProjectTopics
import com.intellij.injected.editor.VirtualFileWindow
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootEvent
import com.intellij.openapi.roots.ModuleRootListener
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.ModificationTracker
import com.intellij.openapi.util.SimpleModificationTracker
import com.intellij.psi.*
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.testFramework.LightVirtualFile
import com.intellij.util.messages.MessageBusConnection
import com.intellij.util.messages.Topic
import org.move.cli.MoveProjectsService
import org.move.cli.moveProjects
import org.move.lang.MoveFile
import org.move.lang.MoveFileType
import org.move.lang.core.psi.MvPsiManager.Companion.isIgnorePsiEvents
import org.move.lang.core.psi.MvPsiTreeChangeEvent.*

/** Don't subscribe directly or via plugin.xml lazy listeners. Use [MvPsiManager.subscribeMoveStructureChange] */
private val MOVE_STRUCTURE_CHANGE_TOPIC: Topic<MoveStructureChangeListener> = Topic.create(
    "MOVE_STRUCTURE_CHANGE_TOPIC",
    MoveStructureChangeListener::class.java,
    Topic.BroadcastDirection.TO_PARENT
)

/** Don't subscribe directly or via plugin.xml lazy listeners. Use [MvPsiManager.subscribeRustPsiChange] */
private val MOVE_PSI_CHANGE_TOPIC: Topic<MovePsiChangeListener> = Topic.create(
    "MOVE_PSI_CHANGE_TOPIC",
    MovePsiChangeListener::class.java,
    Topic.BroadcastDirection.TO_PARENT
)

interface MvPsiManager {
    /**
     * A project-global modification tracker that increments on each PSI change that can affect
     * name resolution or type inference. It will be incremented with a change of most types of
     * PSI element excluding function bodies (expressions and statements)
     */
    val moveStructureModificationTracker: ModificationTracker

    fun incStructureModificationCount()

    /** This is an instance method because [MvPsiManager] should be created prior to event subscription */
    fun subscribeMoveStructureChange(connection: MessageBusConnection, listener: MoveStructureChangeListener) {
        connection.subscribe(MOVE_STRUCTURE_CHANGE_TOPIC, listener)
    }

    /** This is an instance method because [MvPsiManager] should be created prior to event subscription */
    fun subscribeRustPsiChange(connection: MessageBusConnection, listener: MovePsiChangeListener) {
        connection.subscribe(MOVE_PSI_CHANGE_TOPIC, listener)
    }

    companion object {
        private val IGNORE_PSI_EVENTS: Key<Boolean> = Key.create("IGNORE_PSI_EVENTS")

        fun <T> withIgnoredPsiEvents(psi: PsiFile, f: () -> T): T {
            setIgnorePsiEvents(psi, true)
            try {
                return f()
            } finally {
                setIgnorePsiEvents(psi, false)
            }
        }

        fun isIgnorePsiEvents(psi: PsiFile): Boolean =
            psi.getUserData(IGNORE_PSI_EVENTS) == true

        private fun setIgnorePsiEvents(psi: PsiFile, ignore: Boolean) {
            val virtualFile = psi.virtualFile ?: return
            check(virtualFile is LightVirtualFile)

            psi.putUserData(IGNORE_PSI_EVENTS, if (ignore) true else null)
        }
    }
}

interface MoveStructureChangeListener {
    fun moveStructureChanged(file: PsiFile?, changedElement: PsiElement?)
}

interface MovePsiChangeListener {
    fun movePsiChanged(file: PsiFile, element: PsiElement, isStructureModification: Boolean)
}

class MvPsiManagerImpl(val project: Project) : MvPsiManager, Disposable {

    override val moveStructureModificationTracker = SimpleModificationTracker()

    init {
        PsiManager.getInstance(project).addPsiTreeChangeListener(CacheInvalidator(), this)

        project.messageBus.connect().subscribe(ProjectTopics.PROJECT_ROOTS, object : ModuleRootListener {
            override fun rootsChanged(event: ModuleRootEvent) {
                incStructureModificationCount()
            }
        })
        project.messageBus.connect().subscribe(
            MoveProjectsService.MOVE_PROJECTS_TOPIC,
            MoveProjectsService.MoveProjectsListener { _, _ ->
                incStructureModificationCount()
            })
    }

    override fun dispose() {}

    inner class CacheInvalidator : MvPsiTreeChangeAdapter() {
        override fun handleEvent(event: MvPsiTreeChangeEvent) {
            val element = when (event) {
                is ChildRemoval.Before -> event.child
                is ChildRemoval.After -> event.parent
                is ChildReplacement.Before -> event.oldChild
                is ChildReplacement.After -> event.newChild
                is ChildAddition.After -> event.child
                is ChildMovement.After -> event.child
                is ChildrenChange.After -> if (!event.isGenericChange) event.parent else return
                is PropertyChange.After -> {
                    when (event.propertyName) {
                        PsiTreeChangeEvent.PROP_UNLOADED_PSI, PsiTreeChangeEvent.PROP_FILE_TYPES -> {
                            incStructureModificationCount()
                            return
                        }
                        PsiTreeChangeEvent.PROP_WRITABLE -> return
                        else -> event.element ?: return
                    }
                }
                else -> return
            }

            val file = event.file

            // if file is null, this is an event about VFS changes
            if (file == null) {
                if (element is MoveFile ||
                    element is PsiDirectory
                    && project.moveProjects.findMoveProject(element.virtualFile) != null
                ) {
                    incRustStructureModificationCount(element as? MoveFile, element as? MoveFile)
                }
            } else {
                if (file.fileType != MoveFileType) return
                if (isIgnorePsiEvents(file)) return

                val isWhitespaceOrComment = element is PsiComment || element is PsiWhiteSpace
                if (isWhitespaceOrComment) {
                    // Whitespace/comment changes are not meaningful
                    return
                }

                // Most of events means that some element *itself* is changed, but ChildrenChange means
                // that changed some of element's children, not the element itself. In this case
                // we should look up for ModificationTrackerOwner a bit differently
                val isChildrenChange = event is ChildrenChange || event is ChildRemoval.After

                updateModificationCount(file, element, isChildrenChange)
            }
        }
    }

    private fun updateModificationCount(
        file: PsiFile,
        psi: PsiElement,
        isChildrenChange: Boolean,
    ) {
        // We find the nearest parent item or macro call (because macro call can produce items)
        // If found item implements RsModificationTrackerOwner, we increment its own
        // modification counter. Otherwise we increment global modification counter.
        //
        // So, if something is changed inside a function except an item, we will only
        // increment the function local modification counter.
        //
        // It may not be intuitive that if we change an item inside a function,
        // like this struct: `fn foo() { struct Bar; }`, we will increment the
        // global modification counter instead of function-local. We do not care
        // about it because it is a rare case and implementing it differently
        // is much more difficult.

        val owner = if (DumbService.isDumb(project)) null else psi.findModificationTrackerOwner(!isChildrenChange)

        // Whitespace/comment changes are meaningful for macros only
        // (b/c they affect range mappings and body hashes)
//        if (isWhitespaceOrComment) return

        val isStructureModification = owner == null || !owner.incModificationCount(psi)

//        if (!isStructureModification && owner is RsMacroCall &&
//            (!isMacroExpansionModeNew || !owner.isTopLevelExpansion)
//        ) {
//            return updateModificationCount(file, owner, isChildrenChange = false, isWhitespaceOrComment = false)
//        }

        if (isStructureModification) {
            incRustStructureModificationCount(file, psi)
        }
        project.messageBus.syncPublisher(MOVE_PSI_CHANGE_TOPIC).movePsiChanged(file, psi, isStructureModification)
    }

//    private val isMacroExpansionModeNew
//        get() = project.macroExpansionManagerIfCreated?.macroExpansionMode is MacroExpansionMode.New

    override fun incStructureModificationCount() =
        incRustStructureModificationCount(null, null)

    private fun incRustStructureModificationCount(file: PsiFile? = null, psi: PsiElement? = null) {
        moveStructureModificationTracker.incModificationCount()
        project.messageBus.syncPublisher(MOVE_STRUCTURE_CHANGE_TOPIC).moveStructureChanged(file, psi)
    }
}

val Project.movePsiManager: MvPsiManager get() = service()

/** @see MvPsiManager.moveStructureModificationTracker */
val Project.moveStructureModificationTracker: ModificationTracker
    get() = movePsiManager.moveStructureModificationTracker

/**
 * Returns [MvPsiManager.moveStructureModificationTracker] or [PsiModificationTracker.MODIFICATION_COUNT]
 * if `this` element is inside language injection
 */
val MvElement.moveStructureOrAnyPsiModificationTracker: Any
    get() {
        val containingFile = containingFile
        return when (containingFile.virtualFile) {
            // The case of injected language. Injected PSI don't have it's own event system, so can only
            // handle evens from outer PSI. For example, Rust language is injected to Kotlin's string
            // literal. If a user change the literal, we can only be notified that the literal is changed.
            // So we have to invalidate the cached value on any PSI change
            is VirtualFileWindow -> PsiModificationTracker.MODIFICATION_COUNT
            else -> containingFile.project.moveStructureModificationTracker
        }
    }
