package com.minimine.mundo;

public final class Chave {
    public static final int MASCARA_Y = 0xFFFF;

    // chave 2D
    public static final long gerar(final int x, final int z) {
        return ((long)x << 32) | (z & 0xFFFFFFFFL);
    }

    // chave 3D: X nos bits 63-32, Z nos bits 31-16, Y nos bits 15-0
    public static final long gerar3d(final int x, final int y, final int z) {
        return ((long)x << 32) | ((z & 0xFFFFL) << 16) | (y & MASCARA_Y);
    }

    // extratores 2D
    public static final int x(final long chave) {
        return (int)(chave >> 32);
    }

    public static final int z(final long chave) {
        return (int)chave;
    }

    // extratores 3D
    public static final int x3d(final long chave) {
        return (int)(chave >> 32);
    }

    public static final int y3d(final long chave) {
        return (int)(chave & MASCARA_Y);
    }

    public static final int z3d(final long chave) {
        return (short)((chave >> 16) & 0xFFFF);
    }
}
