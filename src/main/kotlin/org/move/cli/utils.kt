package org.move.cli

import com.intellij.execution.ExternalizablePath
import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.vfs.VirtualFile
import org.jdom.Element
import java.nio.file.Path
import java.nio.file.Paths

fun Element.writeString(name: String, value: String) {
    val opt = Element("option")
    opt.setAttribute("name", name)
    opt.setAttribute("value", value)
    addContent(opt)
}

fun Element.readString(name: String): String? =
    children
        .find { it.name == "option" && it.getAttributeValue("name") == name }
        ?.getAttributeValue("value")


fun Element.writePath(name: String, value: Path?) {
    if (value != null) {
        val s = ExternalizablePath.urlValue(value.toString())
        writeString(name, s)
    }
}

fun Element.readPath(name: String): Path? {
    return readString(name)?.let { Paths.get(ExternalizablePath.localPathValue(it)) }
}

fun splitOnDoubleDash(arguments: List<String>): Pair<List<String>, List<String>> {
    val idx = arguments.indexOf("--")
    if (idx == -1) return arguments to emptyList()
    return arguments.take(idx) to arguments.drop(idx + 1)
}

val Module.moveProjectRoot: VirtualFile?
    get() = ModuleRootManager.getInstance(this).contentRoots.firstOrNull {
        it.findChild(MvConstants.MANIFEST_FILE) != null
    }
