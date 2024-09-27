import compilerTools.TextColor;
import java.awt.Color;

%%
%class LexerColor
%type TextColor
%char
%{
    private TextColor textColor(long start, int size, Color color){
        return new TextColor((int) start, size, color);
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

/* Comentarios o espacios en blanco */
{Comentario} { return textColor(yychar, yylength(), new Color(146, 146, 146)); }

/* Numero */
{Entero} | {Flotante} { /*Ignorar*/ }
true | false { return textColor(yychar, yylength(), new Color(53,191,212)); }

/* Tipo de dato */
float | int | bool { return textColor(yychar, yylength(), new Color(53,191,212)); }

/* Operadores de agrupacion */
"(" | ")" | "{" | "}" { return textColor(yychar, yylength(), new Color(163,183,235)); }

/* Signos de puntuacion*/
"," | ";" { return textColor(yychar, yylength(), new Color(21,5,18)); }

/* Operador deeasignacion */
"=" { return textColor(yychar, yylength(), new Color(163,183,235)); }

/* Palabra reservada*/
program | read | write { return textColor(yychar, yylength(), new Color(53,191,212)); }

/* Estructura if*/
if | else { return textColor(yychar, yylength(), new Color(240,210,211)); }

/* Operadores logicos*/
not | and | or { return textColor(yychar, yylength(), new Color(83,3,75)); }

/* Operadors de comparacion */
"==" | "<" | ">" | ">=" | "<=" { return textColor(yychar, yylength(), new Color(163,183,235)); }

/* Operaciones Aritmeticas */
"+" | "-" | "*" | "/" | "^" { return textColor(yychar, yylength(), new Color(163,183,235)); }

/* break */
break { return textColor(yychar, yylength(), new Color(240,210,211)); }

/*  CICLO*/
while | do | until { return textColor(yychar, yylength(), new Color(240,210,211)); }

/* Identificador */
{Identificador} { /* Ignorar */ }

{EspacioEnBlanco} { /*Ignorar*/ }
. { /* Ignorar */ }