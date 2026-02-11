package com.minimine.utils.ruidos;

import com.minimine.utils.Mat;
import com.minimine.Inicio;
import com.badlogic.gdx.Gdx;

public class SimplexNoise3D {
    public static final float F3 = 1.0f / 3.0f;
    public static final float G3 = 1.0f / 6.0f;

    // gradientes(vetores de direção)
    public static final int[][] GRAD3 = {
        {1,1,0}, {-1,1,0}, {1,-1,0}, {-1,-1,0},
        {1,0,1}, {-1,0,1}, {1,0,-1}, {-1,0,-1},
        {0,1,1}, {0,-1,1}, {0,1,-1}, {0,-1,-1}
    };
    // permutação:
    public final int[] p;

    public SimplexNoise3D(long semente) {
		int[] perm = new int[256];
		for(int i = 0; i < 256; i++) perm[i] = i;
		// logica de embaralhamento(fisher-yates + xorshift32)
		long estado = semente;
		if(estado == 0) estado = 0x9E3779B9;

		for(int i = 255; i > 0; i--) {
			// xorshift32 passo
			long z = estado;
			z ^= (z << 13);
			z ^= (z >>> 17);
			z ^= (z << 5);
			estado = z;
			long urnd = z & 0xFFFFFFFFL;
			int j = (int) (urnd % (i + 1)); // em [0, i]

			int tmp = perm[i];
			perm[i] = perm[j];
			perm[j] = tmp;
		}
		this.p = new int[512];
		for(int i = 0; i < 512; i++) {
			this.p[i] = perm[i & 255];
		}
    }

    public float ruido(float xin, float yin, float zin) {
        float s = (xin + yin + zin) * F3;
        int i = Mat.floor(xin + s);
        int j = Mat.floor(yin + s);
        int k = Mat.floor(zin + s);

        float t = (i + j + k) * G3;
        float X0 = i - t;
        float Y0 = j - t;
        float Z0 = k - t;
        float x0 = xin - X0;
        float y0 = yin - Y0;
        float z0 = zin - Z0;

        int i1, j1, k1;
        int i2, j2, k2;

        if(x0 >= y0) {
            if(y0 >= z0) { i1 = 1; j1 = 0; k1 = 0; i2 = 1; j2 = 1; k2 = 0; }
            else if(x0 >= z0) { i1 = 1; j1 = 0; k1 = 0; i2 = 1; j2 = 0; k2 = 1; }
            else { i1 = 0; j1 = 0; k1 = 1; i2 = 1; j2 = 0; k2 = 1; }
        } else {
            if(y0 < z0) { i1 = 0; j1 = 0; k1 = 1; i2 = 0; j2 = 1; k2 = 1; }
            else if(x0 < z0) { i1 = 0; j1 = 1; k1 = 0; i2 = 0; j2 = 1; k2 = 1; }
            else { i1 = 0; j1 = 1; k1 = 0; i2 = 1; j2 = 1; k2 = 0; }
        }
        float x1 = x0 - i1 + G3;
        float y1 = y0 - j1 + G3;
        float z1 = z0 - k1 + G3;
        float x2 = x0 - i2 + 2.0f * G3;
        float y2 = y0 - j2 + 2.0f * G3;
        float z2 = z0 - k2 + 2.0f * G3;
        float x3 = x0 - 1.0f + 3.0f * G3;
        float y3 = y0 - 1.0f + 3.0f * G3;
        float z3 = z0 - 1.0f + 3.0f * G3;

        int ii = i & 255;
        int jj = j & 255;
        int kk = k & 255;

        float n0, n1, n2, n3;

        float t0 = 0.6f - x0*x0 - y0*y0 - z0*z0;
        if(t0 < 0) n0 = 0.0f;
        else {
            t0 *= t0;
            int gi0 = p[ii + p[jj + p[kk]]] % 12;
            n0 = t0 * t0 * Mat.dot(GRAD3[gi0], x0, y0, z0);
        }
        float t1 = 0.6f - x1*x1 - y1*y1 - z1*z1;
        if(t1 < 0) n1 = 0.0f;
        else {
            t1 *= t1;
            int gi1 = p[ii + i1 + p[jj + j1 + p[kk + k1]]] % 12;
            n1 = t1 * t1 * Mat.dot(GRAD3[gi1], x1, y1, z1);
        }
        float t2 = 0.6f - x2*x2 - y2*y2 - z2*z2;
        if(t2 < 0) n2 = 0.0f;
        else {
            t2 *= t2;
            int gi2 = p[ii + i2 + p[jj + j2 + p[kk + k2]]] % 12;
            n2 = t2 * t2 * Mat.dot(GRAD3[gi2], x2, y2, z2);
        }
        float t3 = 0.6f - x3*x3 - y3*y3 - z3*z3;
        if(t3 < 0) n3 = 0.0f;
        else {
            t3 *= t3;
            int gi3 = p[ii + 1 + p[jj + 1 + p[kk + 1]]] % 12;
            n3 = t3 * t3 * Mat.dot(GRAD3[gi3], x3, y3, z3);
        }
        return 32.0f * (n0 + n1 + n2 + n3);
    }

    // calcula o ruido fractal(FBM) 3D
	public float ruidoFractal(float x, float y, float z, float escala, int octaves, float persis) {
        float total = 0f;
        float amplitude = 1f;
        float maxValor = 0f;
        float lx = x;
        float ly = y;
        float lz = z;

        for(int i = 0; i < octaves; i++) {
            total += ruido(lx, ly, lz) * amplitude;

            maxValor += amplitude;
            amplitude *= persis;
            lx *= 2f;
            ly *= 2f;
            lz *= 2f;
        }
        return total / maxValor;
    }
}
