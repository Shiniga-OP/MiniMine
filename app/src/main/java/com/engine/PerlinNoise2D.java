package com.engine;

public class PerlinNoise2D {
    private static final int[] p = new int[512];
    private static final int[] permutacao = {
        151,160,137,91,90,15,
        131,13,201,95,96,53,194,233,7,225,140,36,103,30,69,142,8,99,37,240,21,10,23,
        190,6,148,247,120,234,75,0,26,197,62,94,252,219,203,117,35,11,32,57,177,33,
        88,237,149,56,87,174,20,125,136,171,168,68,175,74,165,71,134,139,48,27,166,
        77,146,158,231,83,111,229,122,60,211,133,230,220,105,92,41,55,46,245,40,244,
        102,143,54,65,25,63,161,1,216,80,73,209,76,132,187,208,89,18,169,200,196,
        135,130,116,188,159,86,164,100,109,198,173,186,3,64,52,217,226,250,124,123,
        5,202,38,147,118,126,255,82,85,212,207,206,59,227,47,16,58,17,182,189,28,42,
        223,183,170,213,119,248,152,2,44,154,163,70,221,153,101,155,167,43,172,9,
        129,22,39,253,19,98,108,110,79,113,224,232,178,185,112,104,218,246,97,228,
        251,34,242,193,238,210,144,12,191,179,162,241,81,51,145,235,249,14,239,107,
        49,192,214,31,181,199,106,157,184,84,204,176,115,121,50,45,127,4,150,254,
        138,236,205,93,222,114,67,29,24,72,243,141,128,195,78,66,215,61,156,180
    };
    public static final float[][] GRADIENTS = {
        { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 },
        { 1, 1 }, { -1, 1 }, { 1, -1 }, { -1, -1 }
    };

    static {
        for (int i = 0; i < 256; i++) {
            p[i] = permutacao[i];
            p[i + 256] = permutacao[i];
        }
    }

    public static float ruido(float x, float z, int seed) {
        int X = ((int)Math.floor(x) + seed) & 255;
        int Z = ((int)Math.floor(z) + seed) & 255;
        float xf = x - (int)Math.floor(x);
        float zf = z - (int)Math.floor(z);

        float u = fade(xf);
        float v = fade(zf);

        int A = p[X] + Z;
        int B = p[X + 1] + Z;

        float gradAA = grad(p[A], xf, zf);
        float gradBA = grad(p[B], xf - 1, zf);
        float gradAB = grad(p[A + 1], xf, zf - 1);
        float gradBB = grad(p[B + 1], xf - 1, zf - 1);

        float lerpX1 = lerp(u, gradAA, gradBA);
        float lerpX2 = lerp(u, gradAB, gradBB);
        return lerp(v, lerpX1, lerpX2);
    }

    public static float fade(float t) {
        return t * t * t * (t * (t * 6 - 15) + 10);
    }

    public static float lerp(float t, float a, float b) {
        return a + t * (b - a);
    }

    public static float grad(int hash, float x, float y) {
        int h = hash & 7;
        float[] g = GRADIENTS[h];
        return g[0] * x + g[1] * y;
    }
}
