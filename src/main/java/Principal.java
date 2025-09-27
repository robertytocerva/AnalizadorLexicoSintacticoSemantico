import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;

// REPL: lee hasta "yaquedo"
public final class Principal {
    public static void main(String[] args) throws Exception {
        System.out.println("Escribe el código. Termina con una línea que contenga exactamente: yaquedo");
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        StringBuilder sb = new StringBuilder();
        String linea;
        while ((linea = br.readLine()) != null) {
            if (linea.strip().equals("yaquedo")) break;
            sb.append(linea).append("\n");
        }
        String fuente = sb.toString();
        ReporteErrores errores = new ReporteErrores();

        // 1) Léxico
        AnalizadorLexico lx = new AnalizadorLexico(fuente);
        List<Token> tokens = lx.escanear(errores);
        if (errores.hayLexico()) { errores.imprimirTodos(); return; }

        // 2) Sintáctico
        AnalizadorSintactico px = new AnalizadorSintactico(tokens, errores);
        NodoAST.Programa programa = px.parsear();
        if (errores.haySintactico()) { errores.imprimirTodos(); return; }

        // 3) Semántico
        TablaSimbolos ts = new TablaSimbolos();
        AnalizadorSemantico sx = new AnalizadorSemantico(ts, errores);
        sx.analizar(programa);
        if (errores.haySemantico()) { errores.imprimirTodos(); return; }

        // Éxito
        System.out.println("OK: léxico ✓, sintáctico ✓, semántico ✓");
        System.out.println("\nAST:");
        System.out.println(NodoAST.aTexto(programa));
        System.out.println("\n" + ts.imprimir());

        // 4) Preguntar por traducción a 3 direcciones
        System.out.print("¿Desea traducir a código intermedio en cuádruplos? [s/n]: ");
        String resp = br.readLine();
        if (resp != null && resp.trim().toLowerCase().startsWith("s")) {
            GeneradorIntermedio gi = new GeneradorIntermedio();
            List<GeneradorIntermedio.Cuadruplo> cod = gi.generar(programa);
            System.out.println();
            System.out.println(GeneradorIntermedio.comoTexto(cod));
        }
    }
}
