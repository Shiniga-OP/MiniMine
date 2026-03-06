package com.minimine.utils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
/*
 * analisador de JSON, sem dependencias externas, compativel com Java 7
 * suporta: objetos, arrays, strings, numeros(int/double), boolean, null
 
 * uso:
 *   Object val = MJson.analisar(texto);
 *   Map<String, Object> obj = MJson.praObjeto(val);
 *   List<Object> arr = MJson.praArray(val);
 *   String s = MJson.praString(val);
 *   int i = MJson.praInt(val);
 *   double d = MJson.praDouble(val);
 *   boolean b = MJson.praBool(val);
 */
public final class MJson {
    public static Object analisar(String json) {
        if(json == null) throw new RuntimeException("json nulo");
        Estado e = new Estado(json.trim());
        Object val = lerValor(e);
        e.pularEspacos();
        if(e.pos < e.codigo.length()) throw new RuntimeException("conteudo inesperado apos raiz na posicao " + e.pos);
        return val;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> praObjeto(Object v) {
        if(v instanceof Map) return (Map<String, Object>)v;
        throw new RuntimeException("esperado objeto, encontrado: " + tipo(v));
    }

    @SuppressWarnings("unchecked")
    public static List<Object> praArray(Object v) {
        if(v instanceof List) return (List<Object>)v;
        throw new RuntimeException("esperado array, encontrado: " + tipo(v));
    }

    public static String praString(Object v) {
        if(v instanceof String) return (String)v;
        throw new RuntimeException("esperado string, encontrado: " + tipo(v));
    }

    public static int praInt(Object v) {
        if(v instanceof Long) return (int)(long)(Long)v;
        if(v instanceof Double) return (int)(double)(Double)v;
        throw new RuntimeException("esperado numero, encontrado: " + tipo(v));
    }

    public static double praDouble(Object v) {
        if(v instanceof Double) return (Double)v;
        if(v instanceof Long) return (double)(long)(Long)v;
        throw new RuntimeException("esperado numero, encontrado: " + tipo(v));
    }

    public static float praFloat(Object v) {
        return (float)praDouble(v);
    }

    public static boolean praBool(Object v) {
        if(v instanceof Boolean) return (Boolean)v;
        throw new RuntimeException("esperado boolean, encontrado: " + tipo(v));
    }

    // retorna null sem lançar excecao se a chave não existir
    public static Object obter(Map<String, Object> obj, String chave) {
        return obj.get(chave);
    }

    // retorna valorPadrao se a chave não existir ou o valor for null
    public static String obterString(Map<String, Object> obj, String chave, String valorPadrao) {
        Object v = obj.get(chave);
        return (v instanceof String) ? (String)v : valorPadrao;
    }

    public static int obterInt(Map<String, Object> obj, String chave, int valorPadrao) {
        Object v = obj.get(chave);
        if(v == null) return valorPadrao;
        return praInt(v);
    }

    public static double obterDouble(Map<String, Object> obj, String chave, double valorPadrao) {
        Object v = obj.get(chave);
        if(v == null) return valorPadrao;
        return praDouble(v);
    }

    public static float obterFloat(Map<String, Object> obj, String chave, float valorPadrao) {
        Object v = obj.get(chave);
        if(v == null) return valorPadrao;
        return praFloat(v);
    }

    public static boolean obterBool(Map<String, Object> obj, String chave, boolean valorPadrao) {
        Object v = obj.get(chave);
        if(v == null) return valorPadrao;
        return praBool(v);
    }

    // === estado interno do analisador ===
    public static final class Estado {
        public final String codigo;
        public int pos;
		
        Estado(String codigo) {
			this.codigo = codigo;
			this.pos = 0;
		}

        char atual() {
            if(pos >= codigo.length()) throw new RuntimeException("fim inesperado do JSON");
            return codigo.charAt(pos);
        }

        char consumir() {
            char c = atual();
            pos++;
            return c;
        }

        void consumirEsperado(char esperado) {
            char c = consumir();
            if(c != esperado) throw new RuntimeException("esperado '" + esperado + "', encontrado '" + c + "' na posicao " + (pos - 1));
        }

        void pularEspacos() {
            while(pos < codigo.length()) {
                char c = codigo.charAt(pos);
                if(c == ' ' || c == '\t' || c == '\n' || c == '\r') pos++;
                else break;
            }
        }
    }

    // === analisadores internos ===
    public static Object lerValor(Estado e) {
        e.pularEspacos();
        char c = e.atual();
        if(c == '{') return lerObjeto(e);
        if(c == '[') return lerArray(e);
        if(c == '"') return lerString(e);
        if(c == 't' || c == 'f') return lerBoolean(e);
        if(c == 'n') return lerNull(e);
        if(c == '-' || (c >= '0' && c <= '9')) return lerNumero(e);
        throw new RuntimeException("caractere inesperado '" + c + "' na posicao " + e.pos);
    }

    public static Map<String, Object> lerObjeto(Estado e) {
        Map<String, Object> mapa = new LinkedHashMap<String, Object>();
        e.consumirEsperado('{');
        e.pularEspacos();
        if(e.atual() == '}') { e.consumir(); return mapa; }

        while(true) {
            e.pularEspacos();
            String chave = lerString(e);
            e.pularEspacos();
            e.consumirEsperado(':');
            Object valor = lerValor(e);
            mapa.put(chave, valor);
            e.pularEspacos();
            char prox = e.atual();
            if(prox == '}') {
				e.consumir();
				break;
			}
            if(prox == ',') {
				e.consumir();
				continue;
			}
            throw new RuntimeException("esperado ',' ou '}', encontrado '" + prox + "' na posicao " + e.pos);
        }
        return mapa;
    }

    public static List<Object> lerArray(Estado e) {
        List<Object> lista = new ArrayList<Object>();
        e.consumirEsperado('[');
        e.pularEspacos();
        if(e.atual() == ']') { e.consumir(); return lista; }

        while(true) {
            lista.add(lerValor(e));
            e.pularEspacos();
            char prox = e.atual();
            if(prox == ']') {
				e.consumir();
				break;
			}
            if(prox == ',') {
				e.consumir();
				continue;
			}
            throw new RuntimeException("esperado ',' ou ']', encontrado '" + prox + "' na posicao " + e.pos);
        }
        return lista;
    }

    public static String lerString(Estado e) {
        e.consumirEsperado('"');
        StringBuilder sb = new StringBuilder();
        while(true) {
            char c = e.consumir();
            if(c == '"') break;
            if(c == '\\') {
                char esc = e.consumir();
                switch(esc) {
                    case '"':  sb.append('"');  break;
                    case '\\': sb.append('\\'); break;
                    case '/':  sb.append('/');  break;
                    case 'n':  sb.append('\n'); break;
                    case 'r':  sb.append('\r'); break;
                    case 't':  sb.append('\t'); break;
                    case 'b':  sb.append('\b'); break;
                    case 'f':  sb.append('\f'); break;
                    case 'u':  sb.append(lerUnicode(e)); break;
                    default: throw new RuntimeException("escape invalido: \\" + esc);
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public static char lerUnicode(Estado e) {
        StringBuilder hex = new StringBuilder(4);
        for(int i = 0; i < 4; i++) hex.append(e.consumir());
        try {
            return (char)Integer.parseInt(hex.toString(), 16);
        } catch(NumberFormatException ex) {
            throw new RuntimeException("sequencia unicode invalida: \\u" + hex);
        }
    }

    public static Object lerNumero(Estado e) {
        StringBuilder sb = new StringBuilder();
        boolean decimal = false;
        if(e.atual() == '-') sb.append(e.consumir());
        while(e.pos < e.codigo.length()) {
            char c = e.codigo.charAt(e.pos);
			
            if(c >= '0' && c <= '9') sb.append(e.consumir());
            else if(c == '.' || c == 'e' || c == 'E' || c == '+' || c == '-') {
                decimal = true;
                sb.append(e.consumir());
            } else break;
        }
        String s = sb.toString();
        try {
            if(decimal) return Double.parseDouble(s);
            return Long.parseLong(s);
        } catch(NumberFormatException ex) {
            throw new RuntimeException("numero invalido: " + s);
        }
    }

    public static boolean lerBoolean(Estado e) {
        if(e.codigo.startsWith("true",  e.pos)) {
			e.pos += 4;
			return true;
		}
        if(e.codigo.startsWith("false", e.pos)) {
			e.pos += 5;
			return false;
		}
        throw new RuntimeException("valor invalido na posicao " + e.pos);
    }

    public static Object lerNull(Estado e) {
        if(e.codigo.startsWith("null", e.pos)) {
			e.pos += 4;
			return null;
		}
        throw new RuntimeException("valor invalido na posicao " + e.pos);
    }

    // === utilitarios ===
    public static String tipo(Object v) {
        if(v == null) return "null";
        if(v instanceof String) return "string";
        if(v instanceof Long) return "inteiro";
        if(v instanceof Double) return "decimal";
        if(v instanceof Boolean) return "boolean";
        if(v instanceof Map) return "objeto";
        if(v instanceof List) return "array";
        return v.getClass().getSimpleName();
    }
}

