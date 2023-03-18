package org.move.openapiext

import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.Project
import java.awt.Component

val Component.project: Project? get() {
    val context = DataManager.getInstance().getDataContext(this)
    return CommonDataKeys.PROJECT.getData(context)
}
