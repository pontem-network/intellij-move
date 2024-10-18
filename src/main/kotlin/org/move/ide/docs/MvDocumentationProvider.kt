package org.move.ide.docs

import com.intellij.lang.documentation.DocumentationSettings
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.editor.richcopy.HtmlSyntaxInfoUtil
import com.intellij.psi.PsiElement
import io.ktor.util.*
import org.move.ide.MvHighlighter
import org.move.ide.colors.MvColor
import org.move.ide.docs.DocumentationUtils.asAddress
import org.move.ide.docs.DocumentationUtils.asAttribute
import org.move.ide.docs.DocumentationUtils.asConst
import org.move.ide.docs.DocumentationUtils.asEnum
import org.move.ide.docs.DocumentationUtils.asEnumVariant
import org.move.ide.docs.DocumentationUtils.asField
import org.move.ide.docs.DocumentationUtils.asFunction
import org.move.ide.docs.DocumentationUtils.asKeyword
import org.move.ide.docs.DocumentationUtils.asNumber
import org.move.ide.docs.DocumentationUtils.asOperator
import org.move.ide.docs.DocumentationUtils.asSelfParam
import org.move.ide.docs.DocumentationUtils.asString
import org.move.ide.docs.DocumentationUtils.asStruct
import org.move.ide.docs.DocumentationUtils.asTypeParam
import org.move.ide.docs.DocumentationUtils.asVariable
import org.move.ide.docs.DocumentationUtils.colorize
import org.move.ide.docs.DocumentationUtils.doubleColon
import org.move.ide.docs.DocumentationUtils.leftAngle
import org.move.ide.docs.DocumentationUtils.leftBrace
import org.move.ide.docs.DocumentationUtils.leftParen
import org.move.ide.docs.DocumentationUtils.rightAngle
import org.move.ide.docs.DocumentationUtils.rightBrace
import org.move.ide.docs.DocumentationUtils.rightParen
import org.move.ide.docs.DocumentationUtils.spacedColon
import org.move.ide.docs.DocumentationUtils.spacedComma
import org.move.ide.docs.DocumentationUtils.spacedPlus
import org.move.ide.presentation.presentationInfo
import org.move.ide.presentation.text
import org.move.ide.presentation.typeLabel
import org.move.lang.MvElementTypes.DIEM_ADDRESS
import org.move.lang.core.MOVE_BINARY_OPS
import org.move.lang.core.MOVE_KEYWORDS
import org.move.lang.core.MOVE_NUMBER_LITERALS
import org.move.lang.core.MOVE_STRING_LITERALS
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.*
import org.move.lang.core.types.infer.inference
import org.move.lang.core.types.infer.loweredType
import org.move.lang.core.types.ty.Ty
import org.move.lang.core.types.ty.TyUnknown
import org.move.lang.moveProject
import org.move.stdext.joinToWithBuffer
import org.toml.lang.psi.TomlKeySegment

fun MvDocAndAttributeOwner.documentationAsHtml(): String {
    val commentText = docComments().map { it.text }.joinToString("\n")
    return documentationAsHtml(commentText, this)
}

@Suppress("UnstableApiUsage")
object DocumentationUtils {
    private fun loadKey(key: TextAttributesKey): TextAttributes =
        EditorColorsManager.getInstance().globalScheme.getAttributes(key)!!

    val asKeyword get() = loadKey(MvColor.KEYWORD.textAttributesKey)
    val asFunction get() = loadKey(MvColor.FUNCTION.textAttributesKey)
    val asStruct get() = loadKey(MvColor.STRUCT.textAttributesKey)
    val asField get() = loadKey(MvColor.FIELD.textAttributesKey)
    val asEnum get() = loadKey(MvColor.STRUCT.textAttributesKey) // TODO: separate color for enums
    val asEnumVariant get() = loadKey(MvColor.STRUCT.textAttributesKey) // TODO: separate color for enum variants
    val asConst get() = loadKey(MvColor.CONSTANT.textAttributesKey)
    val asTypeParam get() = loadKey(MvColor.TYPE_PARAMETER.textAttributesKey)
    val asVariable get() = loadKey(MvColor.VARIABLE.textAttributesKey)
    val asSelfParam get() = loadKey(MvColor.SELF_PARAMETER.textAttributesKey)
    val asParen get() = loadKey(MvColor.PARENTHESES.textAttributesKey)
    val asColon get() = loadKey(MvColor.SEMICOLON.textAttributesKey)
    val asComma get() = loadKey(MvColor.COMMA.textAttributesKey)
    val asBuiltin get() = loadKey(MvColor.BUILTIN_TYPE.textAttributesKey)
    val asString get() = loadKey(MvColor.STRING.textAttributesKey)
    val asNumber get() = loadKey(MvColor.NUMBER.textAttributesKey)
    val asOperator get() = loadKey(MvColor.OPERATORS.textAttributesKey)
    val asAddress get() = loadKey(MvColor.ADDRESS.textAttributesKey)
    val asPrimitiveTy get() = loadKey(MvColor.PRIMITIVE_TYPE.textAttributesKey)
    val asAttribute get() = loadKey(MvColor.ATTRIBUTE.textAttributesKey)

    val spacedComma = colorize(", ", asComma)
    val spacedColon = colorize(": ", asColon)
    val doubleColon = colorize("::", asColon)
    val spacedPlus = colorize(" + ", asOperator)
    val leftParen = colorize("(", asParen)
    val rightParen = colorize(")", asParen)
    val leftAngle = colorize("<", asParen)
    val rightAngle = colorize(">", asParen)
    val leftBrace = colorize("{", asParen)
    val rightBrace = colorize("}", asParen)

    fun StringBuilder.colorize(text: String?, attrs: TextAttributes, noHtml: Boolean = false) {
        if (noHtml) {
            append(text)
            return
        }

        HtmlSyntaxInfoUtil.appendStyledSpan(
            this, attrs, text?.escapeHTML() ?: "",
            DocumentationSettings.getHighlightingSaturation(false)
        )
    }

    fun colorize(text: String, attrs: TextAttributes, noHtml: Boolean = false): String {
        val sb = StringBuilder()
        sb.colorize(text, attrs, noHtml)
        return sb.toString()
    }
}

fun StringBuilder.generateModuleName(module: MvModule?) {
    append(module?.qualName?.editorText() ?: "unknown")
    append("\n")
}

fun StringBuilder.generateAttributes(attrList: List<MvAttr>) {
    for (attr in attrList) {
        colorize(attr.text, asAttribute)
        append("\n")
    }
}

fun StringBuilder.generateFunction(function: MvFunction) {
    generateModuleName(function.module)
    generateAttributes(function.attrList)

    if (function.isPublic) colorize("public ", asKeyword)
    if (function.isNative) colorize("native ", asKeyword)
    if (function.isEntry) colorize("entry ", asKeyword)

    colorize("fun ", asKeyword)
    colorize(function.name, asFunction)

    function.typeParameterList?.generateDocumentation(this)
    function.functionParameterList?.generateDocumentation(this)
    function.returnType?.generateDocumentation(this)

    function.acquiresType?.generateDocumentation(this)
}

fun StringBuilder.generateModule(module: MvModule) {
    colorize("module ", asKeyword)
    append(module.qualName?.editorText() ?: "unknown")
}

fun StringBuilder.generateStruct(struct: MvStruct) {
    generateModuleName(struct.module)
    generateAttributes(struct.attrList)

    colorize("struct ", asKeyword)
    colorize(struct.name, asStruct)

    struct.typeParameterList?.generateDocumentation(this)
    struct.abilitiesList?.abilityList
        ?.joinToWithBuffer(this, spacedComma, DocumentationUtils.colorize(" has ", asKeyword)) { generateDocumentation(it) }
}

fun StringBuilder.generateNamedField(field: MvNamedFieldDecl) {
    val module = field.fieldOwner.itemElement.module
    val moduleQualifiedName = module.qualName?.editorText() ?: "unknown"
    append(moduleQualifiedName)
    append(doubleColon)

    val ownerName = field.fieldOwner.name ?: angleWrapped("anonymous")
    colorize(ownerName, asStruct)
    append("\n")

    generateAttributes(field.attrList)
    colorize(field.name, asField)
    append(spacedColon)

    val typ = (field.type?.loweredType(false) ?: TyUnknown).renderForDocs(true)
    append(typ)
}

fun StringBuilder.generateShortNamedField(field: MvNamedFieldDecl) {
    colorize(field.name, asField)
    append(spacedColon)
    val typ = (field.type?.loweredType(false) ?: TyUnknown).renderForDocs(true)
    append(typ)
}

fun StringBuilder.generateShortTupleField(field: MvTupleFieldDecl) {
    val typ = field.type.loweredType(false).renderForDocs(true)
    append(typ)
}

fun StringBuilder.generateEnum(enum: MvEnum) {
    generateModuleName(enum.module)
    generateAttributes(enum.attrList)

    colorize("enum ", asKeyword)
    colorize(enum.name, asEnum)

    enum.typeParameterList?.generateDocumentation(this)
    enum.abilitiesList?.abilityList
        ?.joinToWithBuffer(this, spacedComma, DocumentationUtils.colorize(" has ", asKeyword)) { generateDocumentation(it) }
}

fun StringBuilder.generateEnumVariant(variant: MvEnumVariant) {
    val module = variant.enumItem.module
    val moduleQualifiedName = module.qualName?.editorText() ?: "unknown"
    append(moduleQualifiedName)
    append(doubleColon)

    val ownerName = variant.enumItem.name ?: angleWrapped("anonymous")
    colorize(ownerName, asStruct)
    append("\n")

    generateAttributes(variant.attrList)
    colorize(variant.name, asEnumVariant)

    variant.blockFields?.let {
        append(" ")
        append(leftBrace)
        append(" ")

        it.namedFieldDeclList.forEachIndexed { index, field ->
            generateShortNamedField(field)

            if (index != it.namedFieldDeclList.lastIndex) {
                append(spacedComma)
            }
        }

        append(" ")
        append(rightBrace)
    }

    variant.tupleFields?.let {
        append(leftParen)

        it.tupleFieldDeclList.forEachIndexed { index, field ->
            generateShortTupleField(field)

            if (index != it.tupleFieldDeclList.lastIndex) {
                append(spacedComma)
            }
        }

        append(rightParen)
    }
}

fun StringBuilder.generateConst(const: MvConst) {
    generateModuleName(const.module)
    generateAttributes(const.attrList)

    if (const.isPublic) colorize("public ", asKeyword)
    colorize("const ", asKeyword)
    colorize(const.name, asConst)

    append(spacedColon)
    val typ = (const.type?.loweredType(false) ?: TyUnknown).renderForDocs(true)
    append(typ)

    append(" = ")
    append(const.initializer?.expr?.generateDoc())
}

fun StringBuilder.generateSchema(schema: MvSchema) {
    generateModuleName(schema.module)
    generateAttributes(schema.attrList)

    colorize("spec ", asKeyword)
    colorize("schema ", asKeyword)
    colorize(schema.name, asStruct)

    schema.typeParameterList?.generateDocumentation(this)
}

fun StringBuilder.generateTypeParameter(param: MvTypeParameter) {
    val info = param.presentationInfo ?: return
    colorize(info.type, asKeyword)
    append(" ")
    colorize(info.name, asTypeParam)

    val abilities = param.abilityBounds
    if (abilities.isNotEmpty()) {
        abilities.joinToWithBuffer(this, spacedPlus, spacedColon) { generateDocumentation(it) }
    }
}

fun StringBuilder.generatePatBinding(param: MvPatBinding) {
    val info = param.presentationInfo ?: return
    val msl = param.isMslOnlyItem
    val inference = param.inference(msl) ?: return

    colorize(info.type, asKeyword)
    append(" ")
    colorize(info.name, asStruct)
    append(spacedColon)

    val type = inference.getBindingType(param).renderForDocs(true)
    append(type)
}

fun StringBuilder.generateNamedAddress(addr: MvNamedAddress) {
    // TODO: add docs for both [addresses] and [dev-addresses]
    val moveProject = addr.moveProject ?: return
    val refName = addr.referenceName
    val named = moveProject.getNamedAddressTestAware(refName) ?: return
    val address = named.addressLit()?.original ?: angleWrapped("unassigned")

    append(refName)
    append(" = ")
    append("\"")
    append(address)
    append("\"")
}

fun StringBuilder.generateTomlKeySegment(key: TomlKeySegment) {
    val moveProject = key.moveProject ?: return
    val refName = key.name ?: return
    val named = moveProject.getNamedAddressTestAware(refName) ?: return
    val address = named.addressLit()?.original ?: angleWrapped("unassigned")

    colorize("named address", asKeyword)
    append(" ")
    colorize(refName, asAddress)
    append(" = ")
    colorize("\"${address}\"", asString)
}

fun MvExpr.generateDoc(): String {
    val text = text
    val highlighter = MvHighlighter()
    val lexer = highlighter.highlightingLexer
    val builder = StringBuilder()
    lexer.start(text)
    while (lexer.tokenType != null) {
        val type = lexer.tokenType
        val tokenText = lexer.tokenText
        val keyword = MOVE_KEYWORDS.contains(type)
        val number = MOVE_NUMBER_LITERALS.contains(type)
        val string = MOVE_STRING_LITERALS.contains(type)
        val operators = MOVE_BINARY_OPS.contains(type)
        val booleanLiteral = tokenText == "true" || tokenText == "false"
        val address = type == DIEM_ADDRESS

        if (tokenText.contains("\n")) {
            builder.append("...")
            break
        }

        builder.append(
            when {
                keyword        -> colorize(tokenText, asKeyword)
                number         -> colorize(tokenText, asNumber)
                string         -> colorize(tokenText, asString)
                operators      -> colorize(tokenText, asOperator)
                booleanLiteral -> colorize(tokenText, asKeyword)
                address        -> colorize(tokenText, asAddress)
                else           -> tokenText
            }
        )
        lexer.advance()
    }

    return builder.toString()
}

fun MvElement.signature(builder: StringBuilder) {
    val buffer = StringBuilder()
    when (this) {
        is MvFunction       -> buffer.generateFunction(this)
        is MvModule         -> buffer.generateModule(this)
        is MvStruct         -> buffer.generateStruct(this)
        is MvNamedFieldDecl -> buffer.generateNamedField(this)
        is MvEnum           -> buffer.generateEnum(this)
        is MvEnumVariant    -> buffer.generateEnumVariant(this)
        is MvConst          -> buffer.generateConst(this)
        is MvSchema         -> buffer.generateSchema(this)
        else                -> return
    }
    listOf(buffer.toString()).joinTo(builder, "<br>")
}

private fun PsiElement.generateDocumentation(
    buffer: StringBuilder,
    prefix: String = "",
    suffix: String = "",
) {
    buffer += prefix
    when (this) {
        is MvType                  -> buffer += this.loweredType(this.isMsl()).typeLabel(this)

        is MvFunctionParameterList -> {
            val multiline = this.functionParameterList.size > 3
            if (multiline) {
                this.functionParameterList
                    .joinToWithBuffer(buffer, separator = "", prefix = leftParen + "\n", postfix = rightParen) {
                        it.append("   ")
                        generateDocumentation(it)
                        it.append(spacedComma)
                        it.append("\n")
                    }
            } else {
                this.functionParameterList
                    .joinToWithBuffer(buffer, spacedComma, leftParen, rightParen) { generateDocumentation(it) }
            }
        }

        is MvFunctionParameter     -> {
            val name = this.patBinding.identifier.text
            buffer += if (name == "self") colorize("self", asSelfParam) else colorize(name, asVariable)
            this.type?.generateDocumentation(buffer, spacedColon)
        }

        is MvTypeParameterList     ->
            this.typeParameterList
                .joinToWithBuffer(buffer, spacedComma, leftAngle, rightAngle) { generateDocumentation(it) }

        is MvTypeParameter         -> {
            if (this.isPhantom) {
                buffer.colorize("phantom", asKeyword)
                buffer += " "
            }
            buffer.colorize(this.identifier?.text, asTypeParam)
            val bound = this.typeParamBound
            if (bound != null) {
                abilityBounds.joinToWithBuffer(buffer, spacedPlus, spacedColon) { generateDocumentation(it) }
            }
        }

        is MvAbility               -> {
            buffer += this.text
        }

        is MvReturnType            -> this.type?.generateDocumentation(buffer, spacedColon)

        is MvAcquiresType          -> {
            buffer.colorize(" acquires ", asKeyword)
            this.pathTypeList
                .joinToWithBuffer(buffer, spacedComma) { generateDocumentation(it) }
        }
    }
    buffer += suffix
}

private fun angleWrapped(text: String): String = "&lt;$text&gt;"

private fun Ty.renderForDocs(fq: Boolean): String {
    val original = this.text(fq, colors = true)
    return original
}

private operator fun StringBuilder.plusAssign(value: String?) {
    if (value != null) {
        append(value)
    }
}
