package com.minimine.utils.arrays;

public class IntArrayUtil {
    public static final int TAM_INICIAL = 256;
    public int[] arr;
    public int tam = 0;

    public IntArrayUtil() {
        this.arr = new int[TAM_INICIAL];
    }

    public IntArrayUtil(int tamInicial) {
        this.arr = new int[Math.max(tamInicial, TAM_INICIAL)];
    }

    public void add(int i) {
        if(tam == arr.length) {
            int novoTam = arr.length + (arr.length >> 1);
            int[] n = new int[novoTam];
            System.arraycopy(arr, 0, n, 0, arr.length);
            arr = n;
        }
        arr[tam++] = i;
    }

    public int[] praArray() {
        int[] r = new int[tam];
        System.arraycopy(arr, 0, r, 0, tam);
        return r;
    }

    public float memoriaMB() {
        return (arr.length * 4f) / (1024f * 1024f);
    }
}

