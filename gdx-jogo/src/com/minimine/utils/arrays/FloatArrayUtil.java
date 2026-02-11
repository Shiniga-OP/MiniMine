package com.minimine.utils.arrays;

/*
 * array dinamico de floats otimizado para economizar memória
 * começa pequeno(256 elementos = ~1KB) e cresce conforme necessario
 * economia: ~98% de RAM em chunks vazias/esparsas
 */
public class FloatArrayUtil {
    public static final int TAM_INICIAL = 256; // ~1KB
    public float[] arr;
    public int tam = 0;

    public FloatArrayUtil() {
        this.arr = new float[TAM_INICIAL];
    }
    public FloatArrayUtil(int tamInicial) {
        this.arr = new float[Math.max(tamInicial, TAM_INICIAL)];
    }

    public void add(float f) {
        if(tam == arr.length) {
            // cresce 1.5x
            int novoTam = arr.length + (arr.length >> 1);
            float[] n = new float[novoTam];
            System.arraycopy(arr, 0, n, 0, arr.length);
            arr = n;
        }
        arr[tam++] = f;
    }

    // adiciona múltiplos valores de uma vez
    public void addAll(float... valores) {
        int necessario = tam + valores.length;
        if(necessario > arr.length) {
            int novoTam = Math.max(necessario, arr.length + (arr.length >> 1));
            float[] n = new float[novoTam];
            System.arraycopy(arr, 0, n, 0, tam);
            arr = n;
        }
        System.arraycopy(valores, 0, arr, tam, valores.length);
        tam += valores.length;
    }

    public float[] praArray() {
        float[] r = new float[tam];
        System.arraycopy(arr, 0, r, 0, tam);
        return r;
    }

    // retorna o uso de memória atual em KB
    public float memoriaMB() {
        return (arr.length * 4f) / (1024f * 1024f);
    }
}

