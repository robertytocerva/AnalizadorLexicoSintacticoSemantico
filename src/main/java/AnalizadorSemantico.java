
public final class AnalizadorSemantico implements NodoAST.Visitante<NodoAST.DeclVar.Tipo> {

    private final TablaSimbolos ts;
    private final ReporteErrores errores;

    public AnalizadorSemantico(TablaSimbolos ts, ReporteErrores errores) {
        this.ts = ts; this.errores = errores;
    }

    public void analizar(NodoAST.Programa prog) {
        // 1) Registrar declaraciones
        for (NodoAST n : prog.declaraciones) {
            NodoAST.DeclVar d = (NodoAST.DeclVar) n;
            ts.declarar(d.nombre, d.tipo, d.linea, d.columna, errores);
        }
        // 2) Verificar sentencias
        for (NodoAST n : prog.sentencias) {
            if (n instanceof NodoAST.Asignacion a) {
                NodoAST.DeclVar.Tipo tVar = tipoDeVariable(a.nombre, a.linea, a.columna);
                NodoAST.DeclVar.Tipo tExpr = a.valor.aceptar(this);
                if (!compatible(tVar, tExpr)) {
                    errores.semantico(a.linea, a.columna, "asignación incompatible: " + tVar + " = " + tExpr);
                }
            } else if (n instanceof NodoAST.Entrada e) {
                tipoDeVariable(e.nombre, e.linea, e.columna);
            } else if (n instanceof NodoAST.ImprimeExpr ie) {
                ie.expr.aceptar(this);
            } else if (n instanceof NodoAST.ImprimeTexto) {

            } else if (n instanceof NodoAST.Mientras m) {
                NodoAST.DeclVar.Tipo t = m.condicion.aceptar(this);
                if (t == NodoAST.DeclVar.Tipo.CADENA)
                    errores.semantico(m.linea, m.columna, "la condición no puede ser de tipo CADENA");
                for (NodoAST s : m.cuerpo) {

                    if (s instanceof NodoAST.Asignacion a) {
                        NodoAST.DeclVar.Tipo tv = tipoDeVariable(a.nombre, a.linea, a.columna);
                        NodoAST.DeclVar.Tipo te = a.valor.aceptar(this);
                        if (!compatible(tv, te)) errores.semantico(a.linea, a.columna, "asignación incompatible: " + tv + " = " + te);
                    } else if (s instanceof NodoAST.Entrada e) tipoDeVariable(e.nombre, e.linea, e.columna);
                    else if (s instanceof NodoAST.ImprimeExpr ie2) ie2.expr.aceptar(this);
                    else if (s instanceof NodoAST.ImprimeTexto) {}
                }
            }
        }
    }

    // Visitante de expresiones
    public NodoAST.DeclVar.Tipo visitarPrograma(NodoAST.Programa n) { return null; }
    public NodoAST.DeclVar.Tipo visitarDecl(NodoAST.DeclVar n) { return n.tipo; }
    public NodoAST.DeclVar.Tipo visitarAsignacion(NodoAST.Asignacion n) { return n.valor.aceptar(this); }
    public NodoAST.DeclVar.Tipo visitarEntrada(NodoAST.Entrada n) { return tipoDeVariable(n.nombre, n.linea, n.columna); }
    public NodoAST.DeclVar.Tipo visitarImprimeTexto(NodoAST.ImprimeTexto n) { return NodoAST.DeclVar.Tipo.CADENA; }
    public NodoAST.DeclVar.Tipo visitarImprimeExpr(NodoAST.ImprimeExpr n) { return n.expr.aceptar(this); }

    public NodoAST.DeclVar.Tipo visitarMientras(NodoAST.Mientras n) { return n.condicion.aceptar(this); }

    public NodoAST.DeclVar.Tipo visitarBinaria(NodoAST.Binaria n) {
        NodoAST.DeclVar.Tipo i = n.izq.aceptar(this);
        NodoAST.DeclVar.Tipo d = n.der.aceptar(this);
        // Operadores relacionales: devuelven ENTERO (boolean entero)
        if ("< <= > >= == !=".contains(n.op)) return NodoAST.DeclVar.Tipo.ENTERO;
        // Aritméticos
        if (i == NodoAST.DeclVar.Tipo.CADENA || d == NodoAST.DeclVar.Tipo.CADENA) {
            errores.semantico(n.linea, n.columna, "operación aritmética con CADENA");
            return NodoAST.DeclVar.Tipo.ENTERO;
        }
        if (i == NodoAST.DeclVar.Tipo.DOBLE || d == NodoAST.DeclVar.Tipo.DOBLE) return NodoAST.DeclVar.Tipo.DOBLE;
        return NodoAST.DeclVar.Tipo.ENTERO;
    }

    public NodoAST.DeclVar.Tipo visitarVariable(NodoAST.Variable n) {
        return tipoDeVariable(n.nombre, n.linea, n.columna);
    }

    public NodoAST.DeclVar.Tipo visitarLiteral(NodoAST.Literal n) {
        if (n.valor instanceof String) return NodoAST.DeclVar.Tipo.CADENA;
        if (n.valor instanceof Double) return NodoAST.DeclVar.Tipo.DOBLE;
        return NodoAST.DeclVar.Tipo.ENTERO;
    }

    public NodoAST.DeclVar.Tipo visitarAgrupacion(NodoAST.Agrupacion n) { return n.expr.aceptar(this); }

    // ---- utilidades ----
    private NodoAST.DeclVar.Tipo tipoDeVariable(String nombre, int l, int c){
        TablaSimbolos.Simbolo s = ts.buscar(nombre);
        if (s == null) { errores.semantico(l, c, "variable '" + nombre + "' no declarada"); return NodoAST.DeclVar.Tipo.ENTERO; }
        return s.tipo;
    }

    private boolean compatible(NodoAST.DeclVar.Tipo var, NodoAST.DeclVar.Tipo expr) {
        if (var == NodoAST.DeclVar.Tipo.CADENA) return expr == NodoAST.DeclVar.Tipo.CADENA;
        if (var == NodoAST.DeclVar.Tipo.DOBLE) return expr == NodoAST.DeclVar.Tipo.DOBLE || expr == NodoAST.DeclVar.Tipo.ENTERO;
        return expr == NodoAST.DeclVar.Tipo.ENTERO;
    }
}

