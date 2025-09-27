import java.util.ArrayList;
import java.util.List;

// Convierte caracteres en tokens y adjunta línea/columna.
public final class AnalizadorLexico {
    private final String fuente;
    private final List<Token> tokens = new ArrayList<>();
    private int i = 0, linea = 1, columna = 1;

    public AnalizadorLexico(String fuente) { this.fuente = fuente; }

    public List<Token> escanear(ReporteErrores errores) {
        while (!fin()) {
            char c = avanzar();
            switch (c) {
                case ' ': case '\t': columna++; break;
                case '\r': break;
                case '\n': linea++; columna = 1; break;
                case '{': agregar(TipoToken.LLAVE_IZQ, "{"); break;
                case '}': agregar(TipoToken.LLAVE_DER, "}"); break;
                case '(': agregar(TipoToken.PAR_IZQ, "("); break;
                case ')': agregar(TipoToken.PAR_DER, ")"); break;
                case ';': agregar(TipoToken.PUNTOYCOMA, ";"); break;
                case ',': agregar(TipoToken.COMA, ","); break;
                case '+': agregar(TipoToken.MAS, "+"); break;
                case '-': agregar(TipoToken.MENOS, "-"); break;
                case '*': agregar(TipoToken.ASTERISCO, "*"); break;
                case '/':
                    if (coincide('/')) { // comentario //
                        while (!fin() && ver() != '\n') avanzar();
                    } else agregar(TipoToken.SLASH, "/");
                    break;
                case '=':
                    if (coincide('=')) agregar(TipoToken.IGUAL_IGUAL, "==");
                    else agregar(TipoToken.ASIGNAR, "=");
                    break;
                case '<':
                    if (coincide('=')) agregar(TipoToken.MENOR_IGUAL, "<=");
                    else agregar(TipoToken.MENOR, "<");
                    break;
                case '>':
                    if (coincide('=')) agregar(TipoToken.MAYOR_IGUAL, ">=");
                    else agregar(TipoToken.MAYOR, ">");
                    break;
                case '!':
                    if (coincide('=')) agregar(TipoToken.DISTINTO, "!=");
                    else error(errores, "se esperaba '=' después de '!'");
                    break;
                case '"':
                    leerCadena(errores);
                    break;
                default:
                    if (Character.isDigit(c)) { leerNumero(c); }
                    else if (Character.isLetter(c) || c == '_') { leerIdentONombre(c); }
                    else {
                        error(errores, "carácter no reconocido: '" + c + "'");
                        agregar(TipoToken.DESCONOCIDO, String.valueOf(c));
                    }
            }
        }
        tokens.add(new Token(TipoToken.EOF, "", linea, columna));
        return tokens;
    }

    private void leerCadena(ReporteErrores errores) {
        int li = linea, ci = columna;
        StringBuilder sb = new StringBuilder();
        while (!fin() && ver() != '"') {
            char c = avanzar();
            if (c == '\n') { linea++; columna = 1; }
            sb.append(c);
        }
        if (fin()) { errores.lexico(li, ci, "\"", "cadena sin cerrar"); return; }
        avanzar(); // cierra "
        agregar(TipoToken.CADENA, sb.toString(), li, ci);
    }

    private void leerNumero(char primero) {
        int li = linea, ci = columna;
        StringBuilder sb = new StringBuilder();
        sb.append(primero);
        while (!fin() && Character.isDigit(ver())) sb.append(avanzar());
        if (!fin() && ver() == '.') {
            sb.append(avanzar());
            while (!fin() && Character.isDigit(ver())) sb.append(avanzar());
        }
        agregar(TipoToken.NUMERO, sb.toString(), li, ci);
    }

    private void leerIdentONombre(char primero) {
        int li = linea, ci = columna;
        StringBuilder sb = new StringBuilder();
        sb.append(primero);
        while (!fin() && (Character.isLetterOrDigit(ver()) || ver()=='_')) sb.append(avanzar());
        String lex = sb.toString();
        TipoToken tipo = switch (lex) {
            case "xtn" -> TipoToken.XT_N;
            case "xdb" -> TipoToken.XD_B;
            case "xst" -> TipoToken.XS_T;
            case "xI"  -> TipoToken.XI;
            case "xImp"-> TipoToken.XIMP;
            case "xO"  -> TipoToken.XO;
            case "xL"  -> TipoToken.XL;
            default    -> TipoToken.IDENT;
        };
        agregar(tipo, lex, li, ci);
    }

    private void agregar(TipoToken t, String lex) { tokens.add(new Token(t, lex, linea, columna)); }
    private void agregar(TipoToken t, String lex, int li, int ci) { tokens.add(new Token(t, lex, li, ci)); }
    private char ver() { return fuente.charAt(i); }
    private char avanzar() { return fuente.charAt(i++); }
    private boolean fin() { return i >= fuente.length(); }
    private boolean coincide(char esperado) { if (fin() || ver()!=esperado) return false; i++; return true; }

    private void error(ReporteErrores errores, String msg) {
        errores.lexico(linea, columna, "", msg);
    }
}
