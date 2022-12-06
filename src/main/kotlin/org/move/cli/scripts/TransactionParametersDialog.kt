package org.move.cli.scripts

import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.UIBundle
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.*
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import org.move.lang.core.psi.MvFunction
import org.move.lang.core.psi.ext.inferredTy
import org.move.lang.core.psi.typeParameters
import org.move.lang.core.types.infer.InferenceContext
import org.move.lang.core.types.ty.TyAddress
import org.move.lang.core.types.ty.TyBool
import org.move.lang.core.types.ty.TyInteger
import org.move.lang.core.types.ty.TyVector
import javax.swing.JComponent

const val NAME_COLUMNS = 42
const val ARGUMENT_COLUMNS = 36
const val PROFILE_COLUMNS = 24

//class TypeParamsCache : TextCompletionCache<String>,
//                        SimplePersistentStateComponent<TypeParamsCache.State>(State(emptyList())) {
//
//    data class State(var items: MutableList<String>) : BaseState()
//
//    override fun setItems(items: MutableCollection<String>) {
//        // do nothing
//    }
//
//    override fun getItems(prefix: String, parameters: CompletionParameters?): MutableCollection<String> {
//        return state.items.filter { prefix in it }.toMutableList()
//    }
//
//    override fun updateCache(prefix: String, parameters: CompletionParameters?) {
//        TODO("Not yet implemented")
//    }
//
//}

fun Row.ulongTextField(range: ULongRange?): Cell<JBTextField> {
    val result = cell(JBTextField())
        .validationOnInput {
            val value = it.text.toULongOrNull()
            when {
                value == null -> error(UIBundle.message("please.enter.a.number"))
                range != null && value !in range -> error(
                    UIBundle.message(
                        "please.enter.a.number.from.0.to.1",
                        range.first,
                        range.last
                    )
                )
                else -> null
            }
        }
    result.component.putClientProperty("dsl.intText.range", range)

    return result
}

//fun <T : EditorTextField> Cell<T>.bindText(getter: () -> String, setter: (String) -> Unit): Cell<T> {
//    return bind(EditorTextField::getText, EditorTextField::setText, MutableProperty(getter, setter))
//}

class TransactionParametersDialog(
    val scriptFunction: MvFunction,
    val profiles: List<String>,
) : DialogWrapper(scriptFunction.project) {

    var configurationName: String
    var selectedProfile: String?
    val typeParams = mutableMapOf<String, String>()
    val params = mutableMapOf<String, String>()

    init {
        title = "Transaction Parameters"
        configurationName = "Run ${scriptFunction.fqName}"
        selectedProfile = null
        init()
    }

    //    fun typeParamTextField(): TextFieldWithAutoCompletion<String> {
//        return TextFieldWithAutoCompletionWithCache.create(
//            TypeParamsCache(),
//            false,
//            scriptFunction.project,
//            null,
//            true,
//            ""
//        )
//    }
//    fun paramTextField(): LanguageTextField {
//        val textField = LanguageTextField(MoveLanguage, scriptFunction.project, "")
//        return textField
//    }

    override fun createCenterPanel(): JComponent {
        return panel {
            row("Run Configuration name: ") {
                textField()
                    .bindText({ "Run ${scriptFunction.fqName}" }, { configurationName })
                    .columns(NAME_COLUMNS)
                    .horizontalAlign(HorizontalAlign.RIGHT)
            }
            val typeParameters = scriptFunction.typeParameters
            val parameters = scriptFunction.parameterBindings().drop(1)

            if (typeParameters.isNotEmpty() || parameters.isNotEmpty()) {
                separator()
            }

            if (typeParameters.isNotEmpty()) {
                group("Type Arguments") {
                    for (typeParameter in scriptFunction.typeParameters) {
                        val paramName = typeParameter.name ?: continue
                        row(paramName) {
                            textField()
                                .bindText({ "" }, { typeParams[paramName] = it })
                                .columns(ARGUMENT_COLUMNS)
                                .horizontalAlign(HorizontalAlign.RIGHT)
                        }
                    }
                }
            }
            if (parameters.isNotEmpty()) {
                group("Value Arguments") {
                    for (parameter in parameters) {
                        val paramName = parameter.name ?: continue
                        val paramTy = parameter.inferredTy(InferenceContext(false))
                        val paramTyName = when (paramTy) {
                            is TyInteger -> paramTy.kind.name
                            is TyAddress -> "address"
                            is TyBool -> "bool"
                            is TyVector -> "vector"
                            else -> "unknown"
                        }
                        row(paramName) {
                            if (paramTy is TyInteger) {
                                comment(": $paramTyName")
                                ulongTextField(paramTy.ulongRange())
                                    .bindText({ "" }, {
                                        if (it.isNotBlank()) {
                                            params[paramName] = "$paramTyName:$it"
                                        }
                                    })
                                    .columns(ARGUMENT_COLUMNS)
                                    .horizontalAlign(HorizontalAlign.RIGHT)
                            } else {
                                comment(": $paramTyName")
                                textField()
                                    .bindText({ "" }, {
                                        if (it.isNotBlank()) {
                                            params[paramName] = "$paramTyName:$it"
                                        }
                                    })
                                    .columns(ARGUMENT_COLUMNS)
                                    .horizontalAlign(HorizontalAlign.RIGHT)
                            }
                        }
                    }
                }
            }
            separator()
            if (profiles.isNotEmpty()) {
                row("Profile:") {
                    comboBox(profiles)
                        .enabled(profiles.size > 1)
                        .bindItem({ profiles[0] }, { selectedProfile = it })
                        .columns(PROFILE_COLUMNS)
                        .horizontalAlign(HorizontalAlign.RIGHT)
                }
            }
        }
    }

    override fun doValidateAll(): MutableList<ValidationInfo> {
        val infos = mutableListOf<ValidationInfo>()
        if (typeParams.size != scriptFunction.typeParameters.size) {
            infos.add(ValidationInfo("Missing type arguments"))
        }
        if (params.size != scriptFunction.parameterBindings().size - 1) {
            infos.add(ValidationInfo("Missing value arguments"))
        }
        return infos

    }
}
