package org.move.cli.project.manifest

import com.intellij.openapi.vfs.VirtualFile

sealed class BaseManifest(open val file: VirtualFile)
