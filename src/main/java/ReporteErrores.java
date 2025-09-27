import java.util.*;

// Acumula errores por fase
public final class ReporteErrores {
    private static final class E {
        final String fase; final int linea, col; final String lexema; final String msg;
        E(String f, int l, int c, String x, String m){ fase=f; linea=l; col=c; lexema=x; msg=m; }
        @Override public String toString(){
            String lx = (lexema==null || lexema.isEmpty()) ? "" : ", lexema '" + lexema + "'";
            return "[" + fase + "] Línea " + linea + ", Col " + col + lx + ": " + msg;
        }
    }

    private final List<E> lexico = new ArrayList<>();
    private final List<E> sintactico = new ArrayList<>();
    private final List<E> semantico = new ArrayList<>();

    public void lexico(int l,int c,String x,String m){ lexico.add(new E("LEXICO",l,c,x,m)); }
    public void sintactico(int l,int c,String m){ sintactico.add(new E("SINTACTICO",l,c,"",m)); }
    public void semantico(int l,int c,String m){ semantico.add(new E("SEMANTICO",l,c,"",m)); }

    public boolean hayErrores(){ return !(lexico.isEmpty() && sintactico.isEmpty() && semantico.isEmpty()); }
    public boolean hayLexico(){ return !lexico.isEmpty(); }
    public boolean haySintactico(){ return !sintactico.isEmpty(); }
    public boolean haySemantico(){ return !semantico.isEmpty(); }

    public void imprimirTodos() {
        lexico.forEach(e -> System.out.println(e));
        sintactico.forEach(e -> System.out.println(e));
        semantico.forEach(e -> System.out.println(e));
    }
}
