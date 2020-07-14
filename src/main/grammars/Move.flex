package org.move.lang;

import com.intellij.psi.tree.IElementType;
import org.move.lang.core.MoveTypes;

%%

%class MoveLexer
%implements FlexLexer
%unicode
%function advance
%type IElementType
%eof{  return;
%eof}

// macro declaration regexes, replaced in the code
EOL_WS = \r | \n | \r\n
LINE_WS = [\ \t]
WHITESPACE_CHAR = {EOL_WS} | {LINE_WS}
WHITESPACE = {WHITESPACE_CHAR}+

//InputCharacter = [^\r\n]

IDENTIFIER = [:jletter:][:jletterdigit:]*
INT_LITERAL = 0 | [1-9][0-9]*

//IntegerLiteral = 0 | [1-9][0-9]*
//MathOperator = \+|-|\*|\/

//%state STRING

%%

//<YYINITIAL> "let"           { return MoveTypes.LET_KEYWORD; }
<YYINITIAL> {
//        {Identifier}        { return MoveTypes.IDENTIFIER; }
        {INT_LITERAL}    { return MoveElementTypes.INT_LITERAL; }

//        "+"                 { return new MoveTokenType("OPERATOR_PLUS"); }
//        "-"                 { return new MoveTokenType("OPERATOR_MINUS"); }

//        {Whitespace}        { /* ignore */}

        [^]                 { throw new Error("Illegal character <" + yytext() + ">"); }
}

//<STRING> {
//        \"                             { yybegin(YYINITIAL); return symbol(sym.STRING_LITERAL, string.toString()); }
//        [^\n\r\"\\]+                   { string.append( yytext() ); }
//        \\t                            { string.append('\t'); }
//        \\n                            { string.append('\n'); }
//
//        \\r                            { string.append('\r'); }
//        \\\"                           { string.append('\"'); }
//        \\                             { string.append('\\'); }
//}