package org.move.ide.hints.type

import com.intellij.codeInsight.hints.*
import com.intellij.codeInsight.hints.ImmediateConfigurable.Case
import com.intellij.codeInsight.hints.presentation.InlayPresentation
import com.intellij.codeInsight.hints.presentation.InsetPresentation
import com.intellij.codeInsight.hints.presentation.MenuOnClickPresentation
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.descendantsOfType
import org.move.lang.MoveLanguage
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.declaration
import org.move.lang.core.psi.ext.endOffset
import org.move.lang.core.psi.ext.isMsl
import org.move.lang.core.types.infer.inference
import org.move.lang.core.types.ty.Ty
import org.move.lang.core.types.ty.TyUnknown
import javax.swing.JComponent
import javax.swing.JPanel

@Suppress("UnstableApiUsage")
class MvInlayTypeHintsProvider : InlayHintsProvider<MvInlayTypeHintsProvider.Settings> {
    override val key: SettingsKey<Settings> get() = KEY

    override val name: String get() = "Types"

    override val previewText: String
        get() = """
            module 0x1::M {
                struct Foo<T1, T2, T3> { x: T1, y: T2, z: T3 }
                fun main() {
                    let foo = Foo { x: 1, y: b"abc", z: true };
                }
            }
            """.trimIndent()

    override val group: InlayGroup get() = InlayGroup.TYPES_GROUP

    override fun createConfigurable(settings: Settings) = object : ImmediateConfigurable {

        override val mainCheckboxText: String
            get() = "Show hints for:"

        /**
         * Each case may have:
         *  * Description provided by [InlayHintsProvider.getProperty].
         *  Property key has `inlay.%[InlayHintsProvider.key].id%.%case.id%` structure
         *
         *  * Preview taken from `resource/inlayProviders/%[InlayHintsProvider.key].id%/%case.id%.rs` file
         */
        override val cases: List<Case>
            get() = listOf(
                Case("Variables", "variables", settings::showForVariables),
                Case("Obvious types", "obvious_types", settings::showObviousTypes),
            )

        override fun createComponent(listener: ChangeListener): JComponent = JPanel()
    }

    override fun createSettings(): Settings = Settings()

    override fun getCollectorFor(
        file: PsiFile,
        editor: Editor,
        settings: Settings,
        sink: InlayHintsSink
    ): InlayHintsCollector {
        val project = file.project
        return object : FactoryInlayHintsCollector(editor) {

            val typeHintsFactory = MvTypeHintsPresentationFactory(factory)

            override fun collect(element: PsiElement, editor: Editor, sink: InlayHintsSink): Boolean {
                if (project.service<DumbService>().isDumb) return true
                if (element !is MvElement) return true

                if (settings.showForVariables) {
                    presentVariable(element)
                }
                return true
            }

            private fun presentVariable(element: MvElement) {
                when (element) {
                    is MvLetStmt -> {
                        if (element.typeAnnotation != null) return
                        val pat = element.pat ?: return
                        presentTypeForPat(pat, element.initializer?.expr)
                    }
                }
            }

            private fun presentTypeForPat(pat: MvPat, expr: MvExpr?) {
                if (!settings.showObviousTypes && isObvious(pat, expr?.declaration)) return

                val msl = pat.isMsl()
                val inference = pat.inference(msl) ?: return
                for (binding in pat.descendantsOfType<MvPatBinding>()) {
                    if (binding.name.startsWith("_"))
                        continue
                    presentTypeForBinding(binding, inference.getBindingType(binding))
                }
            }

            private fun presentTypeForBinding(binding: MvPatBinding, ty: Ty) {
                if (ty is TyUnknown) return
                val presentation = typeHintsFactory.typeHint(ty)
                val offset = binding.endOffset
                val finalPresentation = presentation.withDisableAction(project)
                sink.addInlineElement(
                    offset, false, finalPresentation, false
                )
            }
        }
    }

    private fun InlayPresentation.withDisableAction(project: Project): InsetPresentation = InsetPresentation(
        MenuOnClickPresentation(this, project) {
            listOf(InlayProviderDisablingAction(name, MoveLanguage, project, key))
        }, left = 1
    )

    data class Settings(
        var showForVariables: Boolean = true,
//        var showForLambdas: Boolean = true,
        var showObviousTypes: Boolean = false,
    )

    companion object {
        private val KEY: SettingsKey<Settings> = SettingsKey("move.type.hints")
    }
}

/**
 * Don't show hints in such cases:
 *
 * `let b = MyStruct { x: 42 };`
 */
private fun isObvious(pat: MvPat, declaration: MvElement?): Boolean =
    when (declaration) {
        is MvStruct -> pat is MvPatBinding
        else -> false
    }
