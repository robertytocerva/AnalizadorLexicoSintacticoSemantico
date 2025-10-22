import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

// REPL: tras cuádruplos, ofrece generar .asm (8086 MASM DOS) y guardarlo en disco.
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

        // Cuádruplos
        System.out.print("¿Traducir a cuádruplos? [s/n]: ");
        String resp = br.readLine();
        List<GeneradorIntermedio.Cuadruplo> cuad = null;
        if (resp != null && resp.trim().toLowerCase().startsWith("s")) {
            GeneradorIntermedio gi = new GeneradorIntermedio();
            cuad = gi.generar(programa);
            System.out.println();
            System.out.println(GeneradorIntermedio.comoTexto(cuad));
        }

        // ASM 8086
        if (cuad != null) {
            System.out.print("¿Generar archivo ASM 8086 (MASM) a partir de los cuádruplos? [s/n]: ");
            String respAsm = br.readLine();
            if (respAsm != null && respAsm.trim().toLowerCase().startsWith("s")) {
                System.out.print("Ruta de salida (ej. C:\\\\temp\\\\programa.asm) o vacío para 'programa.asm': ");
                String ruta = br.readLine();
                if (ruta == null || ruta.trim().isEmpty()) ruta = "programa.asm";
                GeneradorASM8086 gasm = new GeneradorASM8086(cuad, ts);
                String asm = gasm.generarASM(programa.nombre);
                java.nio.file.Files.writeString(java.nio.file.Path.of(ruta), asm);
                System.out.println("ASM generado: " + ruta);
            }
        }
    }
}
