package org.move.ide.annotator

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import org.move.cli.settings.moveSettings
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.*
import org.move.lang.core.resolve.ref.Visibility
import org.move.lang.utils.Diagnostic
import org.move.lang.utils.addToHolder
import org.move.stdext.chain

/*
    Augments parser to make the error messages better.
 */
class MvSyntaxErrorAnnotator: MvAnnotatorBase() {
    override fun annotateInternal(element: PsiElement, holder: AnnotationHolder) {
        val moveHolder = MvAnnotationHolder(holder)
        val visitor = object: MvVisitor() {

            override fun visitLitExpr(expr: MvLitExpr) = checkLitExpr(moveHolder, expr)
            override fun visitFunction(o: MvFunction) = checkFunction(moveHolder, o)
            override fun visitSpecFunction(o: MvSpecFunction) = checkSpecFunction(moveHolder, o)
            override fun visitIndexExpr(o: MvIndexExpr) = checkIndexExpr(moveHolder, o)
            override fun visitMethodCall(o: MvMethodCall) = checkMethodCall(moveHolder, o)
            override fun visitEnum(o: MvEnum) = checkEnum(moveHolder, o)
            override fun visitMatchExpr(o: MvMatchExpr) = checkMatchExpr(moveHolder, o)

            override fun visitModule(o: MvModule) {
                checkVisibilityModifiers(moveHolder, o)
            }
        }
        element.accept(visitor)
    }

    private fun checkEnum(holder: MvAnnotationHolder, enum: MvEnum) {
        if (!enum.project.moveSettings.enableMove2) {
            Diagnostic
                .EnumIsNotSupportedInCompilerV1(enum)
                .addToHolder(holder)
        }
    }

    private fun checkMatchExpr(holder: MvAnnotationHolder, matchExpr: MvMatchExpr) {
        if (!matchExpr.project.moveSettings.enableMove2) {
            Diagnostic
                .MatchExprIsNotSupportedInCompilerV1(matchExpr)
                .addToHolder(holder)
        }
    }

    private fun checkFunction(holder: MvAnnotationHolder, function: MvFunction) {
        when {
            function.isEntry -> {
                // no error if #[test_only]
                if (function.hasTestOnlyAttr) return
                // no error if #[test]
                if (function.hasTestAttr) return
                val returnType = function.returnType ?: return
                Diagnostic
                    .EntryFunCannotHaveReturnValue(returnType)
                    .addToHolder(holder)
            }
        }
    }

    private fun checkSpecFunction(holder: MvAnnotationHolder, specFunction: MvSpecFunction) {
        if (specFunction.returnType == null) {
            Diagnostic
                .SpecFunctionRequiresReturnType(specFunction)
                .addToHolder(holder)
        }
    }

    private fun checkIndexExpr(holder: MvAnnotationHolder, indexExpr: MvIndexExpr) {
        // always supported in specs
        if (indexExpr.isMsl()) return
        if (!indexExpr.project.moveSettings.enableIndexExpr) {
            Diagnostic
                .IndexExprIsNotSupportedInCompilerV1(indexExpr)
                .addToHolder(holder)
        }
    }

    private fun checkVisibilityModifiers(
        holder: MvAnnotationHolder,
        module: MvModule
    ) {
        if (!module.project.moveSettings.enablePublicPackage) {
            for (function in module.functionList) {
                val modifier = function.visibilityModifier ?: continue
                if (modifier.hasPackage) {
                    Diagnostic.PublicPackageIsNotSupportedInCompilerV1(modifier)
                        .addToHolder(holder)
                }
            }
            return
        }

        val allFunctions = module.functionList
        val friendFunctions = allFunctions.filter { it.visibility is Visibility.Restricted.Friend }
        val packageFunctions = allFunctions.filter { it.visibility is Visibility.Restricted.Package }

        if (friendFunctions.isNotEmpty() && packageFunctions.isNotEmpty()) {
            for (function in friendFunctions.chain(packageFunctions)) {
                val visibilityModifier = function.visibilityModifier ?: continue
                Diagnostic
                    .PackageAndFriendModifiersCannotBeUsedTogether(visibilityModifier)
                    .addToHolder(holder)
            }
        }
    }

    private fun checkMethodCall(holder: MvAnnotationHolder, methodCall: MvMethodCall) {
        if (!methodCall.project.moveSettings.enableReceiverStyleFunctions) {
            Diagnostic.ReceiverStyleFunctionsIsNotSupportedInCompilerV1(methodCall)
                .addToHolder(holder)
        }
    }

    private fun checkLitExpr(holder: MvAnnotationHolder, litExpr: MvLitExpr) {
        val lit = litExpr.literal
        when (lit) {
            is Literal.HexInteger -> {
                val litValue = lit.element.text
                var actualLitValue = litValue.removePrefix("0x").lowercase()
                val actualLitOffset = lit.element.startOffset + 2
                if (actualLitValue.isEmpty()) {
                    holder.createErrorAnnotation(litExpr, "Invalid hex integer")
                    return
                }
                val match = HEX_INTEGER_WITH_SUFFIX_REGEX.matchEntire(actualLitValue)
                if (match != null) {
                    actualLitValue = match.groups[1]!!.value.lowercase()
                    val (suffix, range) = match.groups[2]!!
                    if (suffix !in ACCEPTABLE_INTEGER_SUFFIXES) {
                        val suffixRange = TextRange.from(actualLitOffset + range.first, suffix.length)
                        holder.createErrorAnnotation(
                            suffixRange,
                            "Invalid hex integer suffix"
                        )
                    }
                }
                for ((i, char) in actualLitValue.toList().withIndex()) {
                    if (char !in ACCEPTABLE_HEX_INTEGER_SYMBOLS) {
                        val offset = actualLitOffset + i
                        holder.createErrorAnnotation(
                            TextRange.from(offset, 1),
                            "Invalid hex integer symbol"
                        )
                    }
                }
            }
            is Literal.Integer -> {
                var litValue = lit.element.text.lowercase()
                val match = INTEGER_WITH_SUFFIX_REGEX.matchEntire(litValue)
                if (match != null) {
                    litValue = match.groups[1]!!.value.lowercase()
                    val (suffix, range) = match.groups[2]!!
                    if (suffix !in ACCEPTABLE_INTEGER_SUFFIXES) {
                        val litOffset = lit.element.startOffset
                        val suffixRange = TextRange.from(litOffset + range.first, suffix.length)
                        holder.createErrorAnnotation(
                            suffixRange,
                            "Invalid integer suffix"
                        )
                    }
                }
                for ((i, char) in litValue.toList().withIndex()) {
                    if (char !in ACCEPTABLE_INTEGER_SYMBOLS) {
                        val offset = lit.element.startOffset + i
                        holder.createErrorAnnotation(
                            TextRange.from(offset, 1),
                            "Invalid integer symbol"
                        )
                    }
                }
            }
            is Literal.HexString -> {
                val litValue = lit.element.text.lowercase()
                if (!litValue.endsWith('"')) {
                    // don't check incomplete hex strings
                    return
                }
                val hexValue = litValue.removePrefix("x\"").removeSuffix("\"")
                val hexValueOffset = lit.element.startOffset + 2

                // check hex string has even number of symbols
                if (hexValue.length % 2 != 0) {
                    holder.createErrorAnnotation(
                        TextRange.from(hexValueOffset, hexValue.length),
                        "Odd number of characters in hex string. " +
                                "Expected 2 hexadecimal digits for each byte"
                    )
                }

                // check all symbols are valid
                for ((i, char) in hexValue.toList().withIndex()) {
                    if (char !in ACCEPTABLE_HEX_SYMBOLS) {
                        val offset = hexValueOffset + i
                        holder.createErrorAnnotation(
                            TextRange.from(offset, 1),
                            "Invalid hex symbol"
                        )
                    }
                }
            }
            else -> Unit
        }
    }

    @Suppress("CompanionObjectInExtension")
    companion object {
        private val INTEGER_WITH_SUFFIX_REGEX =
            Regex("([0-9a-zA-Z_]+)(u[0-9]{1,4})")
        private val ACCEPTABLE_INTEGER_SYMBOLS =
            setOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '_')
        private val ACCEPTABLE_INTEGER_SUFFIXES =
            setOf("u8", "u16", "u32", "u64", "u128", "u256")
        private val HEX_INTEGER_WITH_SUFFIX_REGEX =
            Regex("([0-9a-zA-Z_]+)*(u[0-9]{1,4})")
        private val ACCEPTABLE_HEX_SYMBOLS =
            setOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f')
        private val ACCEPTABLE_HEX_INTEGER_SYMBOLS = ACCEPTABLE_HEX_SYMBOLS + setOf('_')
    }
}
