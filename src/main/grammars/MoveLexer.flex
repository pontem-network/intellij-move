package org.move.lang;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;

import static com.intellij.psi.TokenType.BAD_CHARACTER;
import static com.intellij.psi.TokenType.WHITE_SPACE;
import static org.move.lang.MoveElementTypes.*;

%%

%{
  private int bracesDepth = 0;

  private boolean isSpec = false;

  private int specBracesDepth = -1;
%}

%{
  public _MoveLexer() {
    this((java.io.Reader)null);
  }
%}

%public
%class _MoveLexer
%implements FlexLexer
%function advance
%type IElementType
%unicode

%s IN_BLOCK_COMMENT
%s IN_SPEC
%s IN_APPLY_TO

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
LINE_COMMENT=("//".*\n)|("//".*\R)

///////////////////////////////////////////////////////////////////////////////////////////////////
// Literals
///////////////////////////////////////////////////////////////////////////////////////////////////
ADDRESS_LITERAL=0x[0-9a-fA-F]{1,40}
BOOL_LITERAL=(true)|(false)
INTEGER_LITERAL=[0-9]+((u8)|(u64)|(u128))?
HEX_STRING_LITERAL=x\"([A-F0-9a-f]*)\"
BYTE_STRING_LITERAL=b\"(.*)\"

IDENTIFIER=[_a-zA-Z][_a-zA-Z0-9]*
FUNCTION_PATTERN_NAME=[*_a-zA-Z][*_a-zA-Z0-9]*

%%
<YYINITIAL,IN_SPEC,IN_APPLY_TO> {
  {WHITE_SPACE}              { return WHITE_SPACE; }

  "{"                        {
          bracesDepth++;
          return L_BRACE; }
  "}"                        {
          bracesDepth--;
          if (bracesDepth < specBracesDepth) {
              yybegin(YYINITIAL);
          }
          return R_BRACE;
      }
  "["                        { return L_BRACK; }
  "]"                        { return R_BRACK; }
  "("                        { return L_PAREN; }
  ")"                        { return R_PAREN; }
  "::"                       { return COLON_COLON; }
  ":"                        { return COLON; }
  ";"                        { return SEMICOLON; }
  ","                        { return COMMA; }
  "."                        { return DOT; }
  "="                        { return EQ; }
  "=="                       { return EQ_EQ; }

  "!"                             { return EXCL; }
  "+"                             { return PLUS; }
  "-"                             { return MINUS; }
}

<YYINITIAL,IN_SPEC> {
  "*"                             { return MUL; }
}

<YYINITIAL,IN_SPEC,IN_APPLY_TO> {
  "/"                             { return DIV; }
  "%"                             { return MODULO; }
  "^"                             { return XOR; }

  "<"                        { return LT; }
  ">"                        { return GT; }
  "&"                        { return AND; }
  "|"                        { return OR; }

  "address"                        {
          if (bracesDepth == 0) {
              return ADDRESS;
          } else {
              return IDENTIFIER;
          }
      }
  "script"                         { return SCRIPT; }
  "module"                         { return MODULE; }
  "const"                          { return CONST; }
  "native"                         { return NATIVE; }
  "public"                         { return PUBLIC; }
  "fun"                            { return FUN; }
  "acquires"                       { return ACQUIRES; }
  "resource"                       { return RESOURCE; }
  "struct"                         { return STRUCT; }
  "use"                            { return USE; }
  "as"                             { return AS; }
  "mut"                            { return MUT; }
  "copyable"                       { return COPYABLE; }
  "copy"                           { return COPY; }
  "move"                           { return MOVE; }
  "return"                         { return RETURN; }
  "abort"                          { return ABORT; }
  "break"                          { return BREAK; }
  "continue"                       { return CONTINUE; }
  "if"                             { return IF; }
  "else"                           { return ELSE; }
  "loop"                           { return LOOP; }
  "while"                          { return WHILE; }
  "let"                            { return LET; }

///////////////////////////////////////////////////////////////////////////////////////////////////
// Literals
///////////////////////////////////////////////////////////////////////////////////////////////////
  "spec"                           {
          specBracesDepth = bracesDepth + 1;
          yybegin(IN_SPEC);
          return SPEC;
      }
  "schema"                         { return SCHEMA; }
  "define"                         { return DEFINE; }

  "/*"                      { yybegin(IN_BLOCK_COMMENT); yypushback(2); }

  {LINE_COMMENT}             { return LINE_COMMENT; }


//  {SCHEMA_APPLY_NAME_PATTERN}      { return SCHEMA_APPLY_NAME_PATTERN; }
}

<IN_SPEC> {
  "local"                          { return LOCAL; }

  "global" / "("|"<"                        { return IDENTIFIER; }
  "global"                        { return GLOBAL; }

  "update" / "("|"<"                        { return IDENTIFIER; }
  "update"                        { return UPDATE; }

  "exists" / "("|"<"                        { return IDENTIFIER; }
  "exists"                        { return EXISTS; }

  "pragma"                         { return PRAGMA; }
  "assume"                         { return ASSUME; }
  "assert"                         { return ASSERT; }
  "aborts_if"                      { return ABORTS_IF; }
  "with"                           { return WITH; }
  "succeeds_if"                    { return SUCCEEDS_IF; }
  "requires"                       { return REQUIRES; }
  "ensures"                        { return ENSURES; }
  "modifies"                       { return MODIFIES; }
  "include"                        { return INCLUDE; }
  "internal"                       { return INTERNAL; }
  "invariant"                      { return INVARIANT; }
  "pack"                           { return PACK; }
  "unpack"                         { return UNPACK; }
  "apply"                          { return APPLY; }
  "to"                             { yybegin(IN_APPLY_TO); return TO; }
  "except"                         { yybegin(IN_APPLY_TO); return EXCEPT; }
  "forall"                         { return FORALL; }
  "in"                             { return IN; }
  "where"                             { return WHERE; }
//
//   {FUNCTION_PATTERN_NAME}      {
//          if (yycharat(-1) == 'o' && yycharat(-2) == 't') {
//              return FUNCTION_PATTERN_NAME;
//            }
//    }
}

<IN_APPLY_TO> {
    {FUNCTION_PATTERN_NAME}     { yybegin(IN_SPEC); return FUNCTION_PATTERN_NAME; }
      [^]       { yybegin(IN_SPEC); }
}

<YYINITIAL,IN_SPEC,IN_APPLY_TO> {
  {ADDRESS_LITERAL}          { return ADDRESS_LITERAL; }
  {BOOL_LITERAL}             { return BOOL_LITERAL; }
  {INTEGER_LITERAL}          { return INTEGER_LITERAL; }
  {HEX_STRING_LITERAL}       { return HEX_STRING_LITERAL; }
  {BYTE_STRING_LITERAL}      { return BYTE_STRING_LITERAL; }
  {IDENTIFIER}               { return IDENTIFIER; }
}

<IN_BLOCK_COMMENT> {
  "*/"    { yybegin(YYINITIAL); return BLOCK_COMMENT; }
  <<EOF>> { yybegin(YYINITIAL); return BLOCK_COMMENT; }
  [^]     { }
}

[^] { return BAD_CHARACTER; }
