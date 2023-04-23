package org.move.utils.ui

import com.intellij.openapi.project.Project
import com.intellij.ui.TextFieldWithAutoCompletion

class CompletionTextField(
    project: Project,
    initialValue: String,
    variants: Collection<String>,
) :
    TextFieldWithAutoCompletion<String>(
        project,
        StringsCompletionProvider(variants, null),
        false,
        initialValue
    )
