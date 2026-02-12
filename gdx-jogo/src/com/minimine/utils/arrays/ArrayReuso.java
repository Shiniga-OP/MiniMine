package com.minimine.utils.arrays;

import java.util.concurrent.ConcurrentLinkedQueue;
/*
 * arrays reutilizaveis pra reduzir GC
 * evita criar novos arrays a cada chunk
 * reduz pausas de GC
 * mantem arrays quentes no cache da CPU
 */
public class ArrayReuso {
    public static final int MAX_TAM = 32; // maximo de arrays no reuso

    // reusos separados por tipo
    public static final ConcurrentLinkedQueue<FloatArrayUtil> reusoFloat = new ConcurrentLinkedQueue<>();
    public static final ConcurrentLinkedQueue<ShortArrayUtil> reusoShort = new ConcurrentLinkedQueue<>();
    public static final ConcurrentLinkedQueue<IntArrayUtil> reusoInt = new ConcurrentLinkedQueue<>();

    // estatisticas(debug)
    public static int totalFloatCriados = 0;
    public static int totalFloatReutilizados = 0;
    public static int totalShortCriados = 0;
    public static int totalShortReutilizados = 0;

    // === obtem um objeto do reuso ou cria um novo se necessario ===
    public static FloatArrayUtil obterFloatArray() {
        FloatArrayUtil array = reusoFloat.poll();
        if(array != null) {
            array.tam = 0; // limpa
            totalFloatReutilizados++;
            return array;
        }
        totalFloatCriados++;
        return new FloatArrayUtil();
    }
	
    public static ShortArrayUtil obterShortArray() {
        ShortArrayUtil array = reusoShort.poll();
        if(array != null) {
            array.tam = 0;
            totalShortReutilizados++;
            return array;
        }
        totalShortCriados++;
        return new ShortArrayUtil();
    }
	
    public static IntArrayUtil obterIntArray() {
        IntArrayUtil array = reusoInt.poll();
        if(array != null) {
            array.tam = 0;
            return array;
        }
        return new IntArrayUtil();
    }

    // === devolve um objeto ao reuso pra reutilização ===
    public static void devolver(FloatArrayUtil array) {
        if(array == null) return;
        if(reusoFloat.size() < MAX_TAM) {
            array.tam = 0; // limpa os dados mas mantem a capacidade
            reusoFloat.offer(array);
        }
        // se o reuso ta cheio, deixa o GC coletar
    }

    public static void devolver(ShortArrayUtil array) {
        if(array == null) return;
        if(reusoShort.size() < MAX_TAM) {
            array.tam = 0;
            reusoShort.offer(array);
        }
    }

    public static void devolver(IntArrayUtil array) {
        if(array == null) return;
        if(reusoInt.size() < MAX_TAM) {
            array.tam = 0;
            reusoInt.offer(array);
        }
    }

    // limpa todos os reuso
    public static void limparPools() {
        reusoFloat.clear();
        reusoShort.clear();
        reusoInt.clear();
    }

    // retorna estatisticas de uso do pool
    public static String estatisticas() {
        float taxaReutilizacaoFloat = totalFloatCriados > 0 ? 
            (totalFloatReutilizados * 100f) / (totalFloatCriados + totalFloatReutilizados) : 0f;
        float taxaReutilizacaoShort = totalShortCriados > 0 ? 
            (totalShortReutilizados * 100f) / (totalShortCriados + totalShortReutilizados) : 0f;

        return String.format(
            "ArrayReuso Estatisticas:\n" +
            "  Float: %d criados, %d reutilizados (%.1f%% reuso), %d no reuso\n" +
            "  Short: %d criados, %d reutilizados (%.1f%% reuso), %d no reuso",
            totalFloatCriados, totalFloatReutilizados, taxaReutilizacaoFloat, reusoFloat.size(),
            totalShortCriados, totalShortReutilizados, taxaReutilizacaoShort, reusoShort.size()
        );
    }
}

