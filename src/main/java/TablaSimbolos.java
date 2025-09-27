import java.util.*;

// Tabla simple con inserción y consulta
public final class TablaSimbolos {
    public static final class Simbolo {
        public final String nombre;
        public final NodoAST.DeclVar.Tipo tipo;
        public final int linea, columna;
        public Simbolo(String n, NodoAST.DeclVar.Tipo t, int l, int c){ nombre=n; tipo=t; linea=l; columna=c; }
    }
    private final Map<String, Simbolo> mapa = new LinkedHashMap<>();

    public boolean declarar(String nombre, NodoAST.DeclVar.Tipo tipo, int l, int c, ReporteErrores errores){
        if (mapa.containsKey(nombre)) {
            errores.semantico(l, c, "variable '" + nombre + "' ya declarada en línea " + mapa.get(nombre).linea);
            return false;
        }
        mapa.put(nombre, new Simbolo(nombre, tipo, l, c));
        return true;
    }
    public Simbolo buscar(String nombre){ return mapa.get(nombre); }

    public String imprimir() {
        StringBuilder sb = new StringBuilder("TABLA DE SÍMBOLOS\n");
        sb.append(String.format("%-12s %-8s %-8s\n","Nombre","Tipo","Línea"));
        for (Simbolo s : mapa.values())
            sb.append(String.format("%-12s %-8s %-8d\n", s.nombre, s.tipo, s.linea));
        return sb.toString();
    }
}
