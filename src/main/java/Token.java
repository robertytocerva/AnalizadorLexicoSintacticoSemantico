public final class Token {
    public final TipoToken tipo;
    public final String lexema;
    public final int linea;
    public final int columna;

    public Token(TipoToken tipo, String lexema, int linea, int columna) {
        this.tipo = tipo;
        this.lexema = lexema;
        this.linea = linea;
        this.columna = columna;
    }

    @Override public String toString() {
        return tipo + "('" + lexema + "')@" + linea + ":" + columna;
    }
}