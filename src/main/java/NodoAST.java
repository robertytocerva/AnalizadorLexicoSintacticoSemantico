import java.util.*;

// Nodos del AST y utilidades de impresión
abstract class NodoAST {
    public final int linea, columna;
    protected NodoAST(int l, int c){ this.linea=l; this.columna=c; }

    public abstract <R> R aceptar(Visitante<R> v);

    interface Visitante<R> {
        R visitarPrograma(Programa n);
        R visitarDecl(DeclVar n);
        R visitarAsignacion(Asignacion n);
        R visitarEntrada(Entrada n);
        R visitarImprimeTexto(ImprimeTexto n);
        R visitarImprimeExpr(ImprimeExpr n);
        R visitarMientras(Mientras n);
        R visitarBinaria(Binaria n);
        R visitarVariable(Variable n);
        R visitarLiteral(Literal n);
        R visitarAgrupacion(Agrupacion n);
    }

    // ---- ÁRBOLES ----
    static final class Programa extends NodoAST {
        public final String nombre;
        public final List<NodoAST> declaraciones;
        public final List<NodoAST> sentencias;
        public Programa(String nombre, List<NodoAST> d, List<NodoAST> s, int l, int c) {
            super(l,c); this.nombre=nombre; this.declaraciones=d; this.sentencias=s;
        }
        public <R> R aceptar(Visitante<R> v){ return v.visitarPrograma(this); }
    }

    static final class DeclVar extends NodoAST {
        public enum Tipo { ENTERO, DOBLE, CADENA }
        public final Tipo tipo;
        public final String nombre;
        public DeclVar(Tipo t, String n, int l, int c){ super(l,c); tipo=t; nombre=n; }
        public <R> R aceptar(Visitante<R> v){ return v.visitarDecl(this); }
    }

    static final class Asignacion extends NodoAST {
        public final String nombre;
        public final Expresion valor;
        public Asignacion(String n, Expresion v, int l, int c){ super(l,c); nombre=n; valor=v; }
        public <R> R aceptar(Visitante<R> v){ return v.visitarAsignacion(this); }
    }

    static abstract class Sentencia extends NodoAST { protected Sentencia(int l,int c){ super(l,c);} }
    static abstract class Expresion extends NodoAST { protected Expresion(int l,int c){ super(l,c);} }

    static final class Entrada extends Sentencia {
        public final String nombre;
        public Entrada(String n,int l,int c){ super(l,c); nombre=n; }
        public <R> R aceptar(Visitante<R> v){ return v.visitarEntrada(this); }
    }

    static final class ImprimeTexto extends Sentencia {
        public final String texto;
        public ImprimeTexto(String t,int l,int c){ super(l,c); texto=t; }
        public <R> R aceptar(Visitante<R> v){ return v.visitarImprimeTexto(this); }
    }

    static final class ImprimeExpr extends Sentencia {
        public final Expresion expr;
        public ImprimeExpr(Expresion e,int l,int c){ super(l,c); expr=e; }
        public <R> R aceptar(Visitante<R> v){ return v.visitarImprimeExpr(this); }
    }

    static final class Mientras extends Sentencia {
        public final Expresion condicion;
        public final List<NodoAST> cuerpo;
        public Mientras(Expresion cond, List<NodoAST> cuerpo, int l, int c){
            super(l,c); this.condicion=cond; this.cuerpo=cuerpo;
        }
        public <R> R aceptar(Visitante<R> v){ return v.visitarMientras(this); }
    }

    static final class Binaria extends Expresion {
        public final Expresion izq, der;
        public final String op;
        public Binaria(Expresion i, String op, Expresion d, int l,int c){
            super(l,c); this.izq=i; this.op=op; this.der=d;
        }
        public <R> R aceptar(Visitante<R> v){ return v.visitarBinaria(this); }
    }

    static final class Variable extends Expresion {
        public final String nombre;
        public Variable(String n,int l,int c){ super(l,c); nombre=n; }
        public <R> R aceptar(Visitante<R> v){ return v.visitarVariable(this); }
    }

    static final class Literal extends Expresion {
        public final Object valor; // Integer, Double, String
        public Literal(Object v,int l,int c){ super(l,c); valor=v; }
        public <R> R aceptar(Visitante<R> v){ return v.visitarLiteral(this); }
    }

    static final class Agrupacion extends Expresion {
        public final Expresion expr;
        public Agrupacion(Expresion e,int l,int c){ super(l,c); expr=e; }
        public <R> R aceptar(Visitante<R> v){ return v.visitarAgrupacion(this); }
    }

    // Impresión legible
    public static String aTexto(NodoAST raiz){
        return raiz.aceptar(new Visitante<String>() {
            private String ident(int n){ return "  ".repeat(n); }
            private String join(List<String> s){ return String.join("\n", s); }

            public String visitarPrograma(Programa n) {
                List<String> lineas = new ArrayList<>();
                lineas.add("Programa " + n.nombre);
                lineas.add("  Declaraciones:");
                for (NodoAST d: n.declaraciones)
                    lineas.add("  - " + d.aceptar(this));
                lineas.add("  Sentencias:");
                for (NodoAST s: n.sentencias)
                    lineas.add("  - " + s.aceptar(this));
                return String.join("\n", lineas);
            }
            public String visitarDecl(DeclVar n) { return "decl " + n.tipo + " " + n.nombre; }
            public String visitarAsignacion(Asignacion n){ return n.nombre+" = "+n.valor.aceptar(this); }
            public String visitarEntrada(Entrada n){ return "xI " + n.nombre; }
            public String visitarImprimeTexto(ImprimeTexto n){ return "xImp \""+n.texto+"\""; }
            public String visitarImprimeExpr(ImprimeExpr n){ return "xO " + n.expr.aceptar(this); }
            public String visitarMientras(Mientras n){
                return "xL ("+n.condicion.aceptar(this)+") { " + n.cuerpo.stream().map(s->s.aceptar(this)).reduce((a,b)->a+"; "+b).orElse("") + " }";
            }
            public String visitarBinaria(Binaria n){ return "(" + n.izq.aceptar(this) + " " + n.op + " " + n.der.aceptar(this) + ")"; }
            public String visitarVariable(Variable n){ return n.nombre; }
            public String visitarLiteral(Literal n){ return String.valueOf(n.valor); }
            public String visitarAgrupacion(Agrupacion n){ return "( " + n.expr.aceptar(this) + " )"; }
        });
    }
}

