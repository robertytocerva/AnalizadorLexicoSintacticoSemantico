import java.util.*;

// Genera cuadruplos

public final class GeneradorIntermedio implements NodoAST.Visitante<String> {

    public static final class Cuadruplo {
        public final String op, arg1, arg2, res;
        public Cuadruplo(String op, String a1, String a2, String r){ this.op=op; this.arg1=a1; this.arg2=a2; this.res=r; }
        @Override public String toString(){
            return String.format("(%-6s, %-8s, %-8s, %-8s)", op, nv(arg1), nv(arg2), nv(res));
        }
        private static String nv(String s){ return s==null? "_": s; }
    }

    private final List<Cuadruplo> codigo = new ArrayList<>();
    private int ctTemp = 1, ctLabel = 1;

    public List<Cuadruplo> generar(NodoAST.Programa p){
        // Declaraciones no emiten código
        for (NodoAST n : p.sentencias) n.aceptar(this);
        return codigo;
    }

    // --- Utilidades ---
    private String t(){ return "t" + (ctTemp++); }
    private String L(){ return "L" + (ctLabel++); }
    private void emite(String op, String a1, String a2, String r){ codigo.add(new Cuadruplo(op,a1,a2,r)); }

    // --- Visitante ---
    public String visitarPrograma(NodoAST.Programa n) { return null; }
    public String visitarDecl(NodoAST.DeclVar n) { return null; }

    public String visitarAsignacion(NodoAST.Asignacion n) {
        String v = n.valor.aceptar(this);
        emite("=", v, null, n.nombre);
        return n.nombre;
    }

    public String visitarEntrada(NodoAST.Entrada n) {
        emite("READ", null, null, n.nombre);
        return n.nombre;
    }

    public String visitarImprimeTexto(NodoAST.ImprimeTexto n) {
        emite("PRINTS", "\""+n.texto+"\"", null, null);
        return null;
    }

    public String visitarImprimeExpr(NodoAST.ImprimeExpr n) {
        String v = n.expr.aceptar(this);
        emite("PRINT", v, null, null);
        return null;
    }

    public String visitarMientras(NodoAST.Mientras n) {
        String Lini = L();
        String Lfin = L();
        emite("LABEL", Lini, null, null);
        String cond = n.condicion.aceptar(this);     // cond en temp (0/1)
        emite("IFZ", cond, null, Lfin);              // if cond == 0 goto Lfin
        for (NodoAST s : n.cuerpo) s.aceptar(this);
        emite("GOTO", Lini, null, null);
        emite("LABEL", Lfin, null, null);
        return null;
    }

    public String visitarBinaria(NodoAST.Binaria n) {
        String a = n.izq.aceptar(this);
        String b = n.der.aceptar(this);
        String r = t();
        // Operadores relacionales producen 0/1
        switch (n.op) {
            case "<": case "<=": case ">": case ">=": case "==": case "!=":
                emite(n.op, a, b, r); // r = (a op b)
                break;
            case "+": case "-": case "*": case "/":
                emite(n.op, a, b, r); // r = a op b
                break;
            default:
                emite("NOP", null, null, null);
        }
        return r;
    }

    public String visitarVariable(NodoAST.Variable n) { return n.nombre; }

    public String visitarLiteral(NodoAST.Literal n) {
        if (n.valor instanceof String) return "\""+n.valor+"\"";
        return String.valueOf(n.valor);
    }

    public String visitarAgrupacion(NodoAST.Agrupacion n) { return n.expr.aceptar(this); }

    // Helper para imprimir como bloque de texto
    public static String comoTexto(List<Cuadruplo> cs){
        StringBuilder sb = new StringBuilder("CÓDIGO INTERMEDIO (cuádruplos)\n");
        int i=1;
        for (Cuadruplo c: cs) sb.append(String.format("%3d: %s%n", i++, c.toString()));
        return sb.toString();
    }
}
