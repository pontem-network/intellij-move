package org.move.lang;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;

import java.util.ArrayList;
import static com.intellij.psi.TokenType.BAD_CHARACTER;
import static com.intellij.psi.TokenType.WHITE_SPACE;
import static org.move.lang.MoveElementTypes.*;

%%

%{
    /**
        * Dedicated storage for starting position of some previously successful
        * match
    */
    private int zzPostponedMarkedPos = -1;

    /**
        * Dedicated nested-comment level counter
    */
    private int zzNestedCommentLevel = 0;
%}

%{
    public _MoveLexer() {
        this((java.io.Reader)null);
    }

      IElementType blockComment() {
          assert(zzNestedCommentLevel == 0);
          yybegin(YYINITIAL);

          zzStartRead = zzPostponedMarkedPos;
          zzPostponedMarkedPos = -1;

          return BLOCK_COMMENT;
      }
%}

%public
%class _MoveLexer
%implements FlexLexer
%function advance
%type IElementType
%unicode

%s IN_BLOCK_COMMENT

///////////////////////////////////////////////////////////////////////////////////////////////////
// Whitespaces
///////////////////////////////////////////////////////////////////////////////////////////////////
EOL_WS           = \n | \r | \r\n
LINE_WS          = [\ \t]
WHITE_SPACE_CHAR = {EOL_WS} | {LINE_WS}
WHITE_SPACE      = {WHITE_SPACE_CHAR}+

///////////////////////////////////////////////////////////////////////////////////////////////////
// Comments
///////////////////////////////////////////////////////////////////////////////////////////////////
//LINE_COMMENT=("//".*)|("//".*)

///////////////////////////////////////////////////////////////////////////////////////////////////
// Literals
///////////////////////////////////////////////////////////////////////////////////////////////////
PLACEHOLDER_ADDRESS_IDENT=\{\{[_a-zA-Z][_a-zA-Z0-9]*\}\}
PLACEHOLDER_ADDRESS_LITERAL=@\{\{[_a-zA-Z][_a-zA-Z0-9]*\}\}

ADDRESS_IDENT=0x[0-9a-fA-F]{1,40}
ADDRESS_LITERAL=@0x[0-9a-fA-F]{1,40}
BECH32_ADDRESS_IDENT=wallet1[A-Z0-9a-z&&[^boi1]]{6,83}
BECH32_ADDRESS_LITERAL=@wallet1[A-Z0-9a-z&&[^boi1]]{6,83}
POLKADOT_ADDRESS_IDENT=[1-9A-HJ-NP-Za-km-z]{40}[1-9A-HJ-NP-Za-km-z]*
POLKADOT_ADDRESS_LITERAL=@[1-9A-HJ-NP-Za-km-z]{40}[1-9A-HJ-NP-Za-km-z]*

BOOL_LITERAL=(true)|(false)
HEX_INTEGER_LITERAL=0x[0-9a-fA-F]+((u8)|(u64)|(u128))?
INTEGER_LITERAL=[0-9]+((u8)|(u64)|(u128))?
HEX_STRING_LITERAL=x\"([A-F0-9a-f]*)\"
BYTE_STRING_LITERAL=b\"(.*)\"

IDENTIFIER=[_a-zA-Z][_a-zA-Z0-9]*
//FUNCTION_PATTERN_NAME=[*_a-zA-Z][*_a-zA-Z0-9]*

%%
<YYINITIAL> {
      {WHITE_SPACE}        { return WHITE_SPACE; }
      "//" .*              { return LINE_COMMENT; }
      "/*"                 {
          yybegin(IN_BLOCK_COMMENT); yypushback(2);
       }
}

<YYINITIAL> {
      // operators
      "{"        { return L_BRACE; }
      "}"        { return R_BRACE; }

      "["        { return L_BRACK; }
      "]"        { return R_BRACK; }
      "("        { return L_PAREN; }
      ")"        { return R_PAREN; }
      "::"       { return COLON_COLON; }
      ":"        { return COLON; }
      ";"        { return SEMICOLON; }
      ","        { return COMMA; }
      "."        { return DOT; }
      "="        { return EQ; }
      "=="       { return EQ_EQ; }
      "!="       { return NOT_EQ; }

      "!"        { return EXCL; }
      "+"        { return PLUS; }
      "-"        { return MINUS; }
      "*"        { return MUL; }
      "/"        { return DIV; }
      "%"        { return MODULO; }
      "^"        { return XOR; }

      "<"        { return LT; }
      ">"        { return GT; }
      "&"        { return AND; }
      "|"        { return OR; }
      "@"        { return AT; }
      "#"        { return HASH; }

      // keywords
      "script"         { return SCRIPT; }
      "module"         { return MODULE; }
      "const"          { return CONST; }
      "native"         { return NATIVE; }
      "public"         { return PUBLIC; }
      "fun"            { return FUN; }
      "acquires"       { return ACQUIRES; }
      "struct"         { return STRUCT; }
      "use"            { return USE; }
      "as"             { return AS; }
      "has"             { return HAS; }
      "mut"            { return MUT; }
      "copy"           { return COPY; }
      "move"           { return MOVE; }
      "return"         { return RETURN; }
      "abort"          { return ABORT; }
      "break"          { return BREAK; }
      "continue"       { return CONTINUE; }
      "if"             { return IF; }
      "else"           { return ELSE; }
      "loop"           { return LOOP; }
      "while"          { return WHILE; }
      "let"            { return LET; }
      "phantom"            { return PHANTOM; }
      "schema"         { return SCHEMA; }
      "spec"           { return SPEC; }

  {PLACEHOLDER_ADDRESS_IDENT}          { return PLACEHOLDER_ADDRESS_IDENT; }
  {PLACEHOLDER_ADDRESS_LITERAL}          { return PLACEHOLDER_ADDRESS_LITERAL; }
  {ADDRESS_LITERAL}          { return ADDRESS_LITERAL; }
  {BECH32_ADDRESS_LITERAL}          { return BECH32_ADDRESS_LITERAL; }
  {POLKADOT_ADDRESS_LITERAL}          { return POLKADOT_ADDRESS_LITERAL; }
  {ADDRESS_IDENT}          { return ADDRESS_IDENT; }
  {BECH32_ADDRESS_IDENT}          { return BECH32_ADDRESS_IDENT; }
  {POLKADOT_ADDRESS_IDENT}          { return POLKADOT_ADDRESS_IDENT; }

  {BOOL_LITERAL}             { return BOOL_LITERAL; }
  {HEX_INTEGER_LITERAL}          { return HEX_INTEGER_LITERAL; }
  {INTEGER_LITERAL}          { return INTEGER_LITERAL; }
  {HEX_STRING_LITERAL}       { return HEX_STRING_LITERAL; }
  {BYTE_STRING_LITERAL}      { return BYTE_STRING_LITERAL; }
  {IDENTIFIER}               { return IDENTIFIER; }
}

<IN_BLOCK_COMMENT> {
  "/*"    {
          if (zzNestedCommentLevel++ == 0)
              zzPostponedMarkedPos = zzStartRead;
      }
  "*/"    {
          if (--zzNestedCommentLevel == 0)
              return blockComment();
      }
  <<EOF>> {
          zzNestedCommentLevel = 0; return blockComment();
 }
  [^]     { }
}

[^] { return BAD_CHARACTER; }
