package com.minimine.utils.arrays;

/**
 * array dinamico de shorts otimizado para economizar memória
 * começa pequeno(256 elementos = ~512 bytes) e cresce conforme necessario
 * usado principalmente para indices de malha
 */
public class ShortArrayUtil {
    public static final int TAM_INICIAL = 256;
    public short[] arr;
    public int tam = 0;

    public ShortArrayUtil() {
        this.arr = new short[TAM_INICIAL];
    }

    public ShortArrayUtil(int tamInicial) {
        this.arr = new short[Math.max(tamInicial, TAM_INICIAL)];
    }

    public void add(short s) {
        if(tam == arr.length) {
            int novoTam = arr.length + (arr.length >> 1);
            short[] n = new short[novoTam];
            System.arraycopy(arr, 0, n, 0, arr.length);
            arr = n;
        }
        arr[tam++] = s;
    }

    public short[] praArray() {
        short[] r = new short[tam];
        System.arraycopy(arr, 0, r, 0, tam);
        return r;
    }

    public float memoriaMB() {
        return (arr.length * 2f) / (1024f * 1024f);
    }
}

