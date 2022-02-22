package org.move.ide.hints.type

import com.intellij.codeInsight.hints.*
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.DumbService
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.descendantsOfType
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.cachedTy
import org.move.lang.core.psi.ext.endOffset
import org.move.lang.core.types.infer.InferenceContext
import org.move.lang.core.types.infer.inferenceCtx
import org.move.lang.core.types.ty.TyUnknown
import javax.swing.JComponent
import javax.swing.JPanel

@Suppress("UnstableApiUsage")
class MvInlayTypeHintsProvider : InlayHintsProvider<MvInlayTypeHintsProvider.Settings> {
    override val key: SettingsKey<Settings> get() = KEY

    override val name: String get() = "Type hints"

    override val previewText: String
        get() = """
            module 0x1::M {
                struct Foo<T1, T2, T3> { x: T1, y: T2, z: T3 }
                fun main() {
                    let foo = Foo { x: 1, y: b"abc", z: true };
                }
            }
            """.trimIndent()

    override fun createConfigurable(settings: Settings): ImmediateConfigurable = object :
        ImmediateConfigurable {

        override val cases: List<ImmediateConfigurable.Case>
            get() = listOf(
                ImmediateConfigurable.Case("Show for variables", "variables", settings::showForVariables),
            )

        override fun createComponent(listener: ChangeListener): JComponent = JPanel()
    }

    override fun createSettings(): Settings = Settings()

    override fun getCollectorFor(
        file: PsiFile,
        editor: Editor,
        settings: Settings,
        sink: InlayHintsSink
    ): InlayHintsCollector =
        object : FactoryInlayHintsCollector(editor) {

            val typeHintsFactory = MvTypeHintsPresentationFactory(factory)

            override fun collect(element: PsiElement, editor: Editor, sink: InlayHintsSink): Boolean {
                if (file.project.service<DumbService>().isDumb) return true
                if (element !is MvLetStatement) return true

                if (settings.showForVariables) {
                    presentLetStatement(element)
                }
                return true
            }

            private fun presentLetStatement(element: MvLetStatement) {
                val pat = element.pat ?: return
                presentTypeForPat(pat)
            }

            private fun presentTypeForPat(pat: MvPat) {
                val ctx = pat.inferenceCtx
                for (binding in pat.descendantsOfType<MvBindingPat>()) {
                    if (binding.identifier.text.startsWith("_")) continue
                    if (binding.cachedTy(ctx) is TyUnknown) continue
                    presentTypeForBinding(binding, ctx)
                }
            }

            private fun presentTypeForBinding(binding: MvBindingPat, ctx: InferenceContext) {
                val presentation = typeHintsFactory.typeHint(binding.cachedTy(ctx))
                sink.addInlineElement(binding.endOffset, false, presentation, false)
            }
        }

    data class Settings(
        var showForVariables: Boolean = true,
    )

    companion object {
        private val KEY: SettingsKey<Settings> = SettingsKey("move.type.hints")
    }
}
