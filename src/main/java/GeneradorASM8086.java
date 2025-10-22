import java.util.*;
import java.util.stream.Collectors;


public final class GeneradorASM8086 {

    private final List<GeneradorIntermedio.Cuadruplo> code;
    private final TablaSimbolos ts;

    // nombres en MAYÚSCULAS para MASM
    private final Set<String> vars  = new LinkedHashSet<>();
    private final Set<String> temps = new LinkedHashSet<>();
    private final Map<String,String> lit2lbl = new LinkedHashMap<>();
    private int relCount = 1;

    public GeneradorASM8086(List<GeneradorIntermedio.Cuadruplo> code, TablaSimbolos ts){
        this.code = code;
        this.ts   = ts;

        // variables declaradas del programa
        for (var s : obtenerSimbolos()) vars.add(s.nombre.toUpperCase());

        // detectar temporales y mapear literales PRINTS
        for (var q : code) {
            scanTemp(q.arg1);
            scanTemp(q.arg2);
            scanTemp(q.res);
            if ("PRINTS".equals(q.op) && q.arg1 != null && q.arg1.startsWith("\"")) {
                lit2lbl.computeIfAbsent(q.arg1, k -> "MSG" + (lit2lbl.size()+1));
            }
        }
    }

    private void scanTemp(String o){
        if (o == null) return;
        // temp válido: t + dígitos (t1, t2, ...). Evita "texto", "temp", etc.
        if (o.length() >= 2 && o.charAt(0) == 't' && o.substring(1).chars().allMatch(Character::isDigit)) {
            String up = o.toUpperCase();
            if (!vars.contains(up)) temps.add(up);
        }
    }

    public String generarASM(String nombrePrograma){
        StringBuilder a = new StringBuilder();

        a.append(".MODEL SMALL\n.STACK 100h\n.DATA\n");

        // Variables
        a.append("  ; Variables declaradas\n");
        for (String n : vars) a.append(String.format("  %-12s DW 0\n", n));

        // Temporales
        a.append("  ; Temporales\n");
        for (String t : temps) if (!vars.contains(t)) a.append(String.format("  %-12s DW 0\n", t));

        // Literales y buffers
        a.append("  ; Literales y buffers DOS\n");
        a.append("  BUFFERIN   DB 32,0,32 DUP('$')\n");
        a.append("  NL         DB 13,10,'$'\n");
        for (var e : lit2lbl.entrySet())
            a.append(String.format("  %-10s DB \"%s\",'$'\n", e.getValue(), escapar(e.getKey())));

        a.append("\n.CODE\nMAIN PROC\n  mov ax,@DATA\n  mov ds,ax\n\n");

        for (var q : code) {
            switch (q.op) {
                case "=":
                    movAX(q.arg1, a);
                    storeAX(q.res, a);
                    break;

                case "+":
                    arit("add", q, a);
                    break;

                case "-":
                    arit("sub", q, a);
                    break;

                case "*":
                    movAX(q.arg1, a);
                    movBX(q.arg2, a);
                    a.append("  imul bx\n");
                    storeAX(q.res, a);
                    break;

                case "/":
                    movAX(q.arg1, a);
                    movBX(q.arg2, a);
                    a.append("  cwd\n  idiv bx\n");
                    storeAX(q.res, a);
                    break;

                case "<":
                case "<=":
                case ">":
                case ">=":
                case "==":
                case "!=": {
                    String Ltrue = "RL" + (relCount++) + "T";
                    String Lend  = "RL" + (relCount-1) + "E";
                    movAX(q.arg1, a);
                    movBX(q.arg2, a);
                    a.append("  cmp ax,bx\n  mov ax,0\n  ")
                            .append(jmpFor(q.op)).append(" ").append(Ltrue).append("\n  jmp ")
                            .append(Lend).append("\n");
                    a.append(Ltrue).append(":\n  mov ax,1\n")
                            .append(Lend).append(":\n");
                    storeAX(q.res, a);
                    break;
                }

                case "LABEL":
                    a.append(q.arg1.toUpperCase()).append(":\n");
                    break;

                case "GOTO":
                    a.append("  jmp ").append(q.arg1.toUpperCase()).append("\n");
                    break;

                case "IFZ":
                    movAX(q.arg1, a);
                    a.append("  cmp ax,0\n  je ").append(q.res.toUpperCase()).append("\n");
                    break;

                case "READ":
                    a.append("  ; READ ").append(q.res == null ? "" : q.res.toUpperCase()).append("\n");
                    a.append("  mov dx,OFFSET BUFFERIN\n  mov ah,0Ah\n  int 21h\n");
                    a.append("  call STRTOINT\n");
                    storeAX(q.res, a);
                    break;

                case "PRINTS": {
                    String lbl = lit2lbl.get(q.arg1);
                    a.append("  mov ah,09h\n  lea dx,").append(lbl).append("\n  int 21h\n");
                    break;
                }

                case "PRINT":
                    a.append("  ; PRINT\n");
                    movAX(q.arg1, a);
                    a.append("  push ax\n  call PRINTINT\n  add sp,2\n");
                    break;

                default:
                    a.append("  ; NOP ").append(q.op).append("\n");
            }
        }

        a.append("\n  mov ax,4C00h\n  int 21h\nMAIN ENDP\n\n");

        // ---- Rutinas de soporte 8086 ----
        a.append("; AX <= entero leído del BUFFERIN\nSTRTOINT PROC\n");
        a.append("  push bx\n  push cx\n  push dx\n  push si\n");
        a.append("  xor ax,ax\n  xor bx,bx\n");
        a.append("  mov si,OFFSET BUFFERIN+2\n");
        a.append("  mov cl,[BUFFERIN+1]\n");
        a.append("  cmp cl,0\n  je sti_fin\n");
        a.append("  mov dl,[si]\n  cmp dl,'-'\n  jne sti_loop\n");
        a.append("  inc si\n  dec cl\n  mov bl,1\n");
        a.append("sti_loop:\n");
        a.append("  cmp cl,0\n  je sti_ap\n");
        a.append("  mov dl,[si]\n  cmp dl,13\n  je sti_ap\n");
        a.append("  sub dl,'0'\n");
        a.append("  cmp dl,9\n  ja sti_ap\n");
        // AX = AX*10 + DL
        a.append("  push dx\n");
        a.append("  mov dx,ax\n");        // DX = AX
        a.append("  shl ax,1\n");         // AX = 2*AX
        a.append("  shl dx,1\n");         // DX = 2*DX  (ahora 4*AX en DX)
        a.append("  shl dx,1\n");         // DX = 8*AX
        a.append("  add ax,dx\n");        // AX = 10*AX
        a.append("  pop dx\n");
        a.append("  mov ah,0\n");
        a.append("  add ax,dx\n");        // + dígito (DL)
        a.append("  inc si\n  dec cl\n  jmp sti_loop\n");
        a.append("sti_ap:\n");
        a.append("  cmp bl,1\n  jne sti_fin\n");
        a.append("  neg ax\n");
        a.append("sti_fin:\n");
        a.append("  pop si\n  pop dx\n  pop cx\n  pop bx\n  ret\nSTRTOINT ENDP\n\n");

        a.append("; imprime AX y salto de línea\nPRINTINT PROC\n");
        a.append("  push ax\n  push bx\n  push cx\n  push dx\n");
        a.append("  mov cx,0\n  cmp ax,0\n  jge pi_conv\n");
        a.append("  mov dl,'-'\n  mov ah,02h\n  int 21h\n  neg ax\n");
        a.append("pi_conv:\n");
        a.append("  mov bx,10\n");
        a.append("pi_loop:\n");
        a.append("  xor dx,dx\n  div bx\n");
        a.append("  add dl,'0'\n  push dx\n  inc cx\n");
        a.append("  cmp ax,0\n  jne pi_loop\n");
        a.append("pi_print:\n");
        a.append("  pop dx\n  mov ah,02h\n  int 21h\n");
        a.append("  loop pi_print\n");
        a.append("  mov ah,09h\n  lea dx,NL\n  int 21h\n");
        a.append("  pop dx\n  pop cx\n  pop bx\n  pop ax\n  ret\nPRINTINT ENDP\n\n");

        a.append("END MAIN\n");
        return a.toString();
    }

    // --- utilidades ---

    private List<TablaSimbolos.Simbolo> obtenerSimbolos(){
        try {
            var f = TablaSimbolos.class.getDeclaredField("mapa");
            f.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, TablaSimbolos.Simbolo> m = (Map<String, TablaSimbolos.Simbolo>) f.get(ts);
            return new ArrayList<>(m.values());
        } catch (Exception e) { return List.of(); }
    }

    private static String jmpFor(String op){
        switch (op) {
            case "<":  return "jl";
            case "<=": return "jle";
            case ">":  return "jg";
            case ">=": return "jge";
            case "==": return "je";
            default:   return "jne"; // "!="
        }
    }

    private static String escapar(String sConComillas){
        String s = sConComillas.substring(1, sConComillas.length()-1);
        return s.replace("\"","'");
    }

    private static boolean esInmediato(String o){
        if (o == null) return false;
        if (o.startsWith("\"")) return true;
        if (o.startsWith("-")) return o.substring(1).chars().allMatch(Character::isDigit);
        return o.chars().allMatch(Character::isDigit);
    }

    private static void movAX(String op, StringBuilder a){
        if (op == null) { a.append("  xor ax,ax\n"); return; }
        if (esInmediato(op))  a.append("  mov ax,").append(op).append("\n");
        else                  a.append("  mov ax,").append(op.toUpperCase()).append("\n");
    }

    private static void movBX(String op, StringBuilder a){
        if (op == null) { a.append("  xor bx,bx\n"); return; }
        if (esInmediato(op))  a.append("  mov bx,").append(op).append("\n");
        else                  a.append("  mov bx,").append(op.toUpperCase()).append("\n");
    }

    private static void storeAX(String dst, StringBuilder a){
        if (dst == null) return;
        a.append("  mov ").append(dst.toUpperCase()).append(",ax\n");
    }

    private static void arit(String ins, GeneradorIntermedio.Cuadruplo q, StringBuilder a){
        movAX(q.arg1, a);
        if (esInmediato(q.arg2)) a.append("  ").append(ins).append(" ax,").append(q.arg2).append("\n");
        else                     a.append("  ").append(ins).append(" ax,").append(q.arg2.toUpperCase()).append("\n");
        storeAX(q.res, a);
    }
}
