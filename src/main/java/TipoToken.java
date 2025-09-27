public enum TipoToken {
    // Palabras clave (según las imágenes)
    XT_N,      // xtn  -> entero
    XD_B,      // xdb  -> doble
    XS_T,      // xst  -> cadena
    XI,        // xI   -> entrada
    XIMP,      // xImp -> imprime literal
    XO,        // xO   -> imprime variable/expr
    XL,        // xL   -> bucle

    IDENT, NUMERO, CADENA,

    // Símbolos
    LLAVE_IZQ, LLAVE_DER,
    PAR_IZQ, PAR_DER,
    PUNTOYCOMA, COMA,
    MAS, MENOS, ASTERISCO, SLASH,
    ASIGNAR,       // =
    MENOR, MAYOR, MENOR_IGUAL, MAYOR_IGUAL, IGUAL_IGUAL, DISTINTO,

    EOF,
    DESCONOCIDO
}