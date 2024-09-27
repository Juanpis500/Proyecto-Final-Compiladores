import compilerTools.Token;

%%
%class Lexer
%type Token
%line
%column
%{
    private Token token(String lexeme, String lexicalComp, int line, int column){
        return new Token(lexeme, lexicalComp, line+1, column+1);
    }
%}
/* Variables básicas de comentarios y espacios */
TerminadorDeLinea = \r|\n|\r\n
EntradaDeCaracter = [^\r\n]
EspacioEnBlanco = {TerminadorDeLinea} | [ \t\f]
ComentarioTradicional = "/*" [^*] ~"*/" | "/*" "*"+ "/"
FinDeLineaComentario = "//" {EntradaDeCaracter}* {TerminadorDeLinea}?
ContenidoComentario = ( [^*] | \*+ [^/*] )*
ComentarioDeDocumentacion = "/**" {ContenidoComentario} "*"+ "/"

/* Comentario */
Comentario = {ComentarioTradicional} | {FinDeLineaComentario} | {ComentarioDeDocumentacion}

/* Identificador */
Letra = [A-Za-zÑñ_ÁÉÍÓÚáéíóúÜü]
Digito = [0-9]
Identificador = {Letra}({Letra}|{Digito})*

/* Número */
Entero = 0 | [1-9][0-9]*
Flotante = {Entero} "." {Digito}{Digito}*

%%

/* espacios en blanco */
{EspacioEnBlanco} { /*Ignorar*/ }

/* Comentarios */
{Comentario} { return token(yytext(), "COMENTARIO", yyline, yycolumn); }

/* Valores */
{Flotante} { return token(yytext(), "FLOTANTE", yyline, yycolumn); }
{Entero} { return token(yytext(), "ENTERO", yyline, yycolumn); }
true | false { return token(yytext(), "BOOLEANO", yyline, yycolumn); }


/* Tipo de dato */
float | int | bool { return token(yytext(), "TIPO_DATO", yyline, yycolumn); }

/* Operadores de agrupacion */
"(" { return token(yytext(), "PARENTESIS_A", yyline, yycolumn); }
")" { return token(yytext(), "PARENTESIS_C", yyline, yycolumn); }
"{" { return token(yytext(), "LLAVE_A", yyline, yycolumn); }
"}" { return token(yytext(), "LLAVE_C", yyline, yycolumn); }

/* Signos de puntuacion*/
"," { return token(yytext(), "COMA", yyline, yycolumn); }
";" { return token(yytext(), "PUNTO_COMA", yyline, yycolumn); }

/* Operador deeasignacion */
"=" { return token(yytext(), "OP_ASIG", yyline, yycolumn); }

/* Palabra reservada*/
program { return token(yytext(), "PROGRAM", yyline, yycolumn); }
read { return token(yytext(), "READ", yyline, yycolumn); }
write { return token(yytext(), "WRITE", yyline, yycolumn); }

/* Estructura if*/
if { return token(yytext(), "IF", yyline, yycolumn); }
else { return token(yytext(), "ELSE", yyline, yycolumn); }
then { return token(yytext(), "THEN", yyline, yycolumn); }
fi { return token(yytext(), "FI", yyline, yycolumn); }

/* Operadores logicos*/
not | and | or { return token(yytext(), "OP_LOGICO", yyline, yycolumn); }

/* Operadores de comparacion */
"==" { return token(yytext(), "IGUAL", yyline, yycolumn); }
"<" { return token(yytext(), "MENOR", yyline, yycolumn); }
">" { return token(yytext(), "MAYOR", yyline, yycolumn); }
">=" { return token(yytext(), "MAYOR_IGUAL", yyline, yycolumn); }
"<=" { return token(yytext(), "MENOR_IGUAL", yyline, yycolumn); }
"!=" { return token(yytext(), "DIFERENTE", yyline, yycolumn); }

/* Operaciones Aritmeticas */
"+" | "-" | "*" | "/" | "^" { return token(yytext(), "OP_ARIT", yyline, yycolumn); }

/* break */
break { return token(yytext(), "BREAK", yyline, yycolumn); }

/*  CICLO*/
while | do | until { return token(yytext(), "CICLO", yyline, yycolumn); }

/* Identificador */
{Identificador} { return token(yytext(), "IDENTIFICADOR", yyline, yycolumn); }


. { return token(yytext(), "ERROR", yyline, yycolumn); }