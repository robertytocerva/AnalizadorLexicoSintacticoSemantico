import java.util.*;

/*
 Forma general admitida:
   Programa -> IDENT '{' Decl* Sent* '}'
   Decl -> (xtn|xdb|xst) IDENT ';'
   Sent -> xI IDENT ';'
         | xImp CADENA ';'
         | xO expr ';'
         | IDENT '=' expr ';'
         | xL '(' cond ')' '{' Sent* '}'
   cond -> expr ( < | <= | > | >= | == | != ) expr
   expr -> term (('+'|'-') term)*
   term -> factor (('*'|'/') factor)*
   factor -> IDENT | NUMERO | '(' expr ')'
*/
public final class AnalizadorSintactico {
    private final List<Token> tks;
    private int p = 0;
    private final ReporteErrores errores;

    public AnalizadorSintactico(List<Token> tokens, ReporteErrores errores) {
        this.tks = tokens; this.errores = errores;
    }

    public NodoAST.Programa parsear() {
        Token nombre = consumir(TipoToken.IDENT, "se esperaba nombre del programa");
        consumir(TipoToken.LLAVE_IZQ, "se esperaba '{' después del nombre");
        List<NodoAST> decls = new ArrayList<>();
        while (coincide(TipoToken.XT_N, TipoToken.XD_B, TipoToken.XS_T)) {
            decls.add(declaracion(previo()));
        }
        List<NodoAST> sents = new ArrayList<>();
        while (!ver(TipoToken.LLAVE_DER) && !fin()) {
            sents.add(sentencia());
        }
        consumir(TipoToken.LLAVE_DER, "se esperaba '}' al final del programa");
        return new NodoAST.Programa(nombre.lexema, decls, sents, nombre.linea, nombre.columna);
    }

    private NodoAST declaracion(Token tipoTk) {
        NodoAST.DeclVar.Tipo tipo = switch (tipoTk.tipo) {
            case XT_N -> NodoAST.DeclVar.Tipo.ENTERO;
            case XD_B -> NodoAST.DeclVar.Tipo.DOBLE;
            default   -> NodoAST.DeclVar.Tipo.CADENA;
        };
        Token id = consumir(TipoToken.IDENT, "se esperaba identificador");
        Token fin = consumir(TipoToken.PUNTOYCOMA, "se esperaba ';' al final de declaración");
        return new NodoAST.DeclVar(tipo, id.lexema, tipoTk.linea, tipoTk.columna);
    }

    private NodoAST sentencia() {
        if (coincide(TipoToken.XI)) {
            Token id = consumir(TipoToken.IDENT, "se esperaba identificador tras xI");
            consumir(TipoToken.PUNTOYCOMA, "se esperaba ';'");
            return new NodoAST.Entrada(id.lexema, id.linea, id.columna);
        }
        if (coincide(TipoToken.XIMP)) {
            Token lit = consumir(TipoToken.CADENA, "se esperaba cadena");
            consumir(TipoToken.PUNTOYCOMA, "se esperaba ';'");
            return new NodoAST.ImprimeTexto(lit.lexema, lit.linea, lit.columna);
        }
        if (coincide(TipoToken.XO)) {
            NodoAST.Expresion e = expresion();
            consumir(TipoToken.PUNTOYCOMA, "se esperaba ';'");
            return new NodoAST.ImprimeExpr(e, e.linea, e.columna);
        }
        if (coincide(TipoToken.XL)) {
            Token par = consumir(TipoToken.PAR_IZQ, "se esperaba '('");
            NodoAST.Expresion cond = condicion();
            consumir(TipoToken.PAR_DER, "se esperaba ')'");
            consumir(TipoToken.LLAVE_IZQ, "se esperaba '{'");
            List<NodoAST> cuerpo = new ArrayList<>();
            while (!ver(TipoToken.LLAVE_DER) && !fin()) cuerpo.add(sentencia());
            consumir(TipoToken.LLAVE_DER, "se esperaba '}'");
            return new NodoAST.Mientras(cond, cuerpo, par.linea, par.columna);
        }
        // asignación
        Token id = consumir(TipoToken.IDENT, "se esperaba sentencia");
        Token igual = consumir(TipoToken.ASIGNAR, "se esperaba '='");
        NodoAST.Expresion e = expresion();
        consumir(TipoToken.PUNTOYCOMA, "se esperaba ';'");
        return new NodoAST.Asignacion(id.lexema, e, id.linea, id.columna);
    }

    private NodoAST.Expresion condicion() {
        NodoAST.Expresion izq = expresion();
        Token op = consumirUnoDe(new TipoToken[]{
                TipoToken.MENOR, TipoToken.MAYOR, TipoToken.MENOR_IGUAL, TipoToken.MAYOR_IGUAL,
                TipoToken.IGUAL_IGUAL, TipoToken.DISTINTO
        }, "se esperaba operador relacional");
        NodoAST.Expresion der = expresion();
        return new NodoAST.Binaria(izq, op.lexema, der, op.linea, op.columna);
    }

    private NodoAST.Expresion expresion() {
        NodoAST.Expresion e = termino();
        while (coincide(TipoToken.MAS, TipoToken.MENOS)) {
            Token op = previo();
            NodoAST.Expresion d = termino();
            e = new NodoAST.Binaria(e, op.lexema, d, op.linea, op.columna);
        }
        return e;
    }

    private NodoAST.Expresion termino() {
        NodoAST.Expresion e = factor();
        while (coincide(TipoToken.ASTERISCO, TipoToken.SLASH)) {
            Token op = previo();
            NodoAST.Expresion d = factor();
            e = new NodoAST.Binaria(e, op.lexema, d, op.linea, op.columna);
        }
        return e;
    }

    private NodoAST.Expresion factor() {
        if (coincide(TipoToken.NUMERO)) {
            Token n = previo();
            if (n.lexema.contains(".")) return new NodoAST.Literal(Double.parseDouble(n.lexema), n.linea, n.columna);
            return new NodoAST.Literal(Integer.parseInt(n.lexema), n.linea, n.columna);
        }
        if (coincide(TipoToken.CADENA)) {
            Token s = previo();
            return new NodoAST.Literal(s.lexema, s.linea, s.columna);
        }
        if (coincide(TipoToken.IDENT)) {
            Token id = previo();
            return new NodoAST.Variable(id.lexema, id.linea, id.columna);
        }
        if (coincide(TipoToken.PAR_IZQ)) {
            Token t = previo();
            NodoAST.Expresion e = expresion();
            consumir(TipoToken.PAR_DER, "se esperaba ')'");
            return new NodoAST.Agrupacion(e, t.linea, t.columna);
        }
        Token tk = actual();
        errores.sintactico(tk.linea, tk.columna, "token inesperado: " + tk.lexema);
        // recuperación mínima: avanzar
        avanzar();
        return new NodoAST.Literal(0, tk.linea, tk.columna);
    }

    // utilidades del parser
    private boolean ver(TipoToken tipo){ return !fin() && actual().tipo==tipo; }
    private boolean fin(){ return actual().tipo==TipoToken.EOF; }
    private Token actual(){ return tks.get(p); }
    private Token previo(){ return tks.get(p-1); }
    private Token avanzar(){ if (!fin()) p++; return previo(); }
    private boolean coincide(TipoToken... tipos){
        for (TipoToken t: tipos) if (ver(t)) { avanzar(); return true; }
        return false;
    }
    private Token consumir(TipoToken tipo, String msg){
        if (ver(tipo)) return avanzar();
        Token tk = actual();
        errores.sintactico(tk.linea, tk.columna, msg);
        return avanzar(); // recuperación
    }
    private Token consumirUnoDe(TipoToken[] tipos, String msg){
        for (TipoToken t: tipos) if (ver(t)) return avanzar();
        Token tk = actual();
        errores.sintactico(tk.linea, tk.columna, msg);
        return avanzar();
    }
}
