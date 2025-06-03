package com.engine;

public class ControleToque {
    static final int MAX_TOQUES = 10;
    static int[] usos = new int[MAX_TOQUES]; // -1 = livre, 1 = botão, 2 = câmera

    static {
        for (int i = 0; i < MAX_TOQUES; i++) usos[i] = -1;
    }

    public static boolean estaLivre(int id) {
        return id >= 0 && id < MAX_TOQUES && usos[id] == -1;
    }

    public static int toquesAtivos() {
        int total = 0;
        for (int i = 0; i < MAX_TOQUES; i++) {
            if (usos[i] != -1) total++;
        }
        return total;
    }

    // Permite reservar se:
    // - toques < 2 OU
    // - já era do mesmo dono
    public static boolean reservar(int id, int dono) {
        if (id < 0 || id >= MAX_TOQUES) return false;
        if (usos[id] == dono) return true;
        if (usos[id] == -1 && toquesAtivos() < 2) {
            usos[id] = dono;
            return true;
        }
        return false;
    }

    public static void liberar(int id) {
        if (id >= 0 && id < MAX_TOQUES) usos[id] = -1;
    }

    public static int dono(int id) {
        if (id >= 0 && id < MAX_TOQUES) return usos[id];
        return -1;
    }
}
