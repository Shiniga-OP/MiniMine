package com.minimine.inventario;

import java.util.ArrayList;
import java.util.List;

public class ReceitaRegistro {

    public static class Receita {
        // grade 3x3, null = vazio
        // indices:
        //   0 1 2
        //   3 4 5
        //   6 7 8
        public final String[] grade;
        public final String resultado;
        public final int quantidade;

        public Receita(String[] grade, String resultado, int quantidade) {
            this.grade = grade;
            this.resultado = resultado;
            this.quantidade = quantidade;
        }
    }

    public static final List<Receita> receitas = new ArrayList<>();

    public static void registrar(String[] grade, String resultado, int quantidade) {
        receitas.add(new Receita(grade, resultado, quantidade));
    }
    /*
     * tenta casar a grade atual com alguma receita registrada
     * normaliza as duas grades(remove bordas vazias) antes de comparar,
     * então receitas 1x1, 2x2, etc funcionam sem precisar preencher o 3x3
     */
    public static Receita combinar(String[] grade) {
        String[] norm = normalizar(grade);
        for(int i = 0; i < receitas.size(); i++) {
            Receita r = receitas.get(i);
            if(gradesIguais(norm, normalizar(r.grade))) return r;
        }
        return null;
    }

    private static String[] normalizar(String[] grade) {
        int minCol = 3, maxCol = -1, minLin = 3, maxLin = -1;
        for(int l = 0; l < 3; l++) {
            for(int c = 0; c < 3; c++) {
                String v = grade[l * 3 + c];
                if(v != null && v.length() > 0) {
                    if(l < minLin) minLin = l;
                    if(l > maxLin) maxLin = l;
                    if(c < minCol) minCol = c;
                    if(c > maxCol) maxCol = c;
                }
            }
        }
        String[] norm = new String[9];
        if(maxLin == -1) return norm;
        for(int l = minLin; l <= maxLin; l++) {
            for(int c = minCol; c <= maxCol; c++) {
                norm[(l - minLin) * 3 + (c - minCol)] = grade[l * 3 + c];
            }
        }
        return norm;
    }

    public static boolean gradesIguais(String[] a, String[] b) {
        for(int i = 0; i < 9; i++) {
            final String va = a[i];
            final String vb = b[i];
            final boolean vazioA = va == null || va.length() == 0;
            final boolean vazioB = vb == null || vb.length() == 0;
            if(vazioA != vazioB) return false;
            if(!vazioA && !va.toString().equals(vb.toString())) return false;
        }
        return true;
    }

    public static void iniciar() {
        registrar(new String[]{
			"tronco", null, null,
			null, null, null,
			null, null, null
		}, "tabua_madeira", 4);
		registrar(new String[]{
			"tabua_madeira", null, null,
			"tabua_madeira", null, null,
			null, null, null
		}, "palito", 4);
		registrar(new String[]{
			"folha", null, null,
			"palito", null, null,
			null, null, null
		}, "tocha", 1);
		registrar(new String[]{
			null, "tabua_madeira", null,
			null, "tabua_madeira", null,
			null, "palito", null
		}, "espada_madeira", 1);
		registrar(new String[]{
			"tabua_madeira", "tabua_madeira", "tabua_madeira",
			null, "palito", null,
			null, "palito", null
		}, "picareta_madeira", 1);
		registrar(new String[]{
			"tabua_madeira", "tabua_madeira", null,
			"tabua_madeira", "palito", null,
			null, "palito", null
		}, "machado_madeira", 1);
    }
}

